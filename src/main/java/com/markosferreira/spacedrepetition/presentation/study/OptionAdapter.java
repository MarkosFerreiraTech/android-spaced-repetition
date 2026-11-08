package com.markosferreira.spacedrepetition.presentation.study;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.databinding.ItemOptionBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Alternativas de uma questao (RF13).
 *
 * <p>Desenha quatro estados visuais por alternativa: neutro, selecionado,
 * correto e errado. Depois do feedback, os cartoes deixam de responder a toque
 * para evitar mudanca de resposta apos a gravacao.
 */
public final class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.OptionViewHolder> {

    /**
     * Acao executada ao escolher uma alternativa.
     */
    public interface OnOptionSelected {
        /**
         * Alternativa escolhida.
         *
         * @param index indice na lista
         */
        void onSelected(int index);
    }

    private final OnOptionSelected listener;
    private List<String> options = new ArrayList<>();
    private int selectedIndex = -1;
    private int correctIndex = -1;
    private boolean revealed;

    /**
     * Cria o adaptador.
     *
     * @param listener acao de escolha
     */
    public OptionAdapter(final OnOptionSelected listener) {
        this.listener = listener;
    }

    /**
     * Define as alternativas da questao atual.
     *
     * @param newOptions textos das alternativas
     */
    public void submit(final List<String> newOptions) {
        this.options = new ArrayList<>(newOptions);
        this.selectedIndex = -1;
        this.correctIndex = -1;
        this.revealed = false;
        notifyDataSetChanged();
    }

    /**
     * Marca a alternativa escolhida, sem revelar o gabarito.
     *
     * @param index alternativa
     */
    public void select(final int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    /**
     * Revela o gabarito: colore correta e errada e bloqueia novos toques.
     *
     * @param correct alternativa correta
     */
    public void reveal(final int correct) {
        this.correctIndex = correct;
        this.revealed = true;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new OptionViewHolder(ItemOptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull final OptionViewHolder holder, final int position) {
        holder.bind(position, options.get(position), position == selectedIndex,
                revealed, position == correctIndex);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    /**
     * ViewHolder de uma alternativa.
     */
    static final class OptionViewHolder extends RecyclerView.ViewHolder {

        private final ItemOptionBinding binding;
        private final OnOptionSelected listener;

        OptionViewHolder(final ItemOptionBinding binding, final OnOptionSelected listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(final int position, final String text, final boolean selected,
                  final boolean revealed, final boolean correct) {
            binding.optionLetter.setText(itemView.getContext()
                    .getString(R.string.study_option_letter, letterFor(position)));
            binding.optionText.setText(text);

            binding.optionCard.setCardBackgroundColor(itemView.getContext()
                    .getColor(backgroundFor(selected, revealed, correct)));

            if (!revealed) {
                binding.optionMark.setVisibility(View.GONE);
                binding.optionCard.setOnClickListener(view -> listener.onSelected(position));
                binding.optionCard.setClickable(true);
                return;
            }

            binding.optionCard.setOnClickListener(null);
            binding.optionCard.setClickable(false);

            if (correct) {
                showMark(R.drawable.ic_check);
            } else if (selected) {
                showMark(R.drawable.ic_close);
            } else {
                binding.optionMark.setVisibility(View.GONE);
            }
        }

        private void showMark(final int drawable) {
            binding.optionMark.setVisibility(View.VISIBLE);
            binding.optionMark.setImageResource(drawable);
        }

        private int backgroundFor(final boolean selected, final boolean revealed,
                                  final boolean correct) {
            if (revealed && correct) {
                return R.color.option_correct_bg;
            }
            if (revealed && selected) {
                return R.color.option_wrong_bg;
            }
            return selected ? R.color.option_selected_bg : R.color.surface;
        }

        private String letterFor(final int position) {
            return String.valueOf((char) ('A' + position));
        }
    }
}
