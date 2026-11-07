package com.markosferreira.spacedrepetition;

import android.app.Application;

import com.markosferreira.spacedrepetition.di.AppContainer;

/**
 * Ponto de entrada do processo.
 *
 * <p>Constroi o grafo de dependencias uma unica vez e o expoe para as
 * activities. Nenhuma activity instancia repositorio, DAO ou parser.
 */
public final class CerebroApplication extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    /**
     * Container de dependencias do processo.
     *
     * @return grafo unico da aplicacao
     */
    public AppContainer container() {
        return container;
    }
}
