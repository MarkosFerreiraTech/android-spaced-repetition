package com.markosferreira.spacedrepetition.presentation.deck;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.Executor;

import com.markosferreira.spacedrepetition.databinding.ItemDeckBinding;

/**
 * Adaptador da lista de baralhos (RF10).
 *
 * <p>Usa {@link ListAdapter} com {@link DiffUtil}: quando a tela recarrega apos
 * uma sessao, apenas os itens alterados sao redesenhados, sem perder a posicao
 * da rolagem nem piscar a lista inteira.
 *
 * <p>Os executores do diff sao injetaveis por um motivo pratico:
 * {@link ListAdapter} calcula a diferenca em uma thread propria e publica o
 * resultado de volta na thread principal. Isso torna imprevisivel o instante em
 * que {@code getItemCount()} muda — em um teste, a contagem pode ser lida antes
 * do diff terminar. Nos testes o mesmo executor sincrono e usado nas duas
 * pontas, entao {@code submitList} conclui antes de retornar.
 */
public final class DeckAdapter extends ListAdapter<DeckItem, DeckAdapter.DeckViewHolder> {

    /**
     * Acao executada ao tocar em um baralho.
     */
    public interface OnDeckClickListener {
        /**
         * Baralho escolhido.
         *
         * @param deckId identificador
         */
        void onDeckClick(String deckId);
    }

    private static final DiffUtil.ItemCallback<DeckItem> DIFF =
            new DiffUtil.ItemCallback<DeckItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull final DeckItem oldItem,
                                               @NonNull final DeckItem newItem) {
                    return oldItem.deckId().equals(newItem.deckId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull final DeckItem oldItem,
                                                  @NonNull final DeckItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnDeckClickListener listener;

    /**
     * Cria o adaptador.
     *
     * @param listener acao de toque no item
     */
    public DeckAdapter(final OnDeckClickListener listener) {
        this(listener, null);
    }

    /**
     * Cria o adaptador informando onde o diff deve rodar.
     *
     * @param listener acao de toque no item
     * @param diffExecutor executor do calculo e da publicacao do diff; quando
     *                     nulo, o {@link ListAdapter} usa os padroes do Android
     */
    public DeckAdapter(final OnDeckClickListener listener, final Executor diffExecutor) {
        super(configureDiff(diffExecutor));
        this.listener = listener;
    }

    /**
     * Monta a configuracao do diff.
     *
     * @param diffExecutor executor unico para calculo e publicacao, ou nulo
     * @return configuracao do {@link ListAdapter}
     */
    private static AsyncDifferConfig<DeckItem> configureDiff(final Executor diffExecutor) {
        final AsyncDifferConfig.Builder<DeckItem> builder = new AsyncDifferConfig.Builder<>(DIFF);
        if (diffExecutor != null) {
            // Mesmo executor nas duas pontas: em teste o diff fica sincrono.
            builder.setBackgroundThreadExecutor(diffExecutor);
            builder.setMainThreadExecutor(diffExecutor);
        }
        return builder.build();
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final ItemDeckBinding binding = ItemDeckBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DeckViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final DeckViewHolder holder, final int position) {
        holder.bind(getItem(position), listener);
    }

    /**
     * ViewHolder de um baralho.
     */
    static final class DeckViewHolder extends RecyclerView.ViewHolder {

        private final ItemDeckBinding binding;

        DeckViewHolder(final ItemDeckBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final DeckItem item, final OnDeckClickListener listener) {
            binding.deckName.setText(item.name());
            binding.deckSubtitle.setText(item.subtitle());

            if (item.dueToday() > 0) {
                binding.chipDue.setVisibility(View.VISIBLE);
                binding.chipDue.setText(String.valueOf(item.dueToday()));
            } else {
                // "Em dia" ja aparece no subtitulo; o chip seria redundante.
                binding.chipDue.setVisibility(View.GONE);
            }

            binding.deckCard.setOnClickListener(view -> listener.onDeckClick(item.deckId()));
            binding.deckCard.setContentDescription(item.name() + ", " + item.subtitle());
        }
    }
}
