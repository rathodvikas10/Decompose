package com.arkivanov.sample.shared.multipane

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.panels.ChildPanelsMode.TRIPLE
import com.arkivanov.decompose.router.panels.Panels
import com.arkivanov.decompose.router.panels.PanelsNavigation
import com.arkivanov.decompose.router.panels.activateExtra
import com.arkivanov.decompose.router.panels.childPanels
import com.arkivanov.decompose.router.panels.isDual
import com.arkivanov.decompose.router.panels.isSingle
import com.arkivanov.decompose.router.panels.navigate
import com.arkivanov.decompose.router.panels.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.reaktive.disposableScope
import com.arkivanov.sample.shared.multipane.author.ArticleAuthorComponent
import com.arkivanov.sample.shared.multipane.author.DefaultArticleAuthorComponent
import com.arkivanov.sample.shared.multipane.database.DefaultArticleDatabase
import com.arkivanov.sample.shared.multipane.details.ArticleDetailsComponent
import com.arkivanov.sample.shared.multipane.details.DefaultArticleDetailsComponent
import com.arkivanov.sample.shared.multipane.list.ArticleListComponent
import com.arkivanov.sample.shared.multipane.list.DefaultArticleListComponent
import com.badoo.reaktive.disposable.scope.DisposableScope
import com.badoo.reaktive.observable.map
import com.badoo.reaktive.observable.notNull
import com.badoo.reaktive.subject.behavior.BehaviorSubject
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

@OptIn(ExperimentalDecomposeApi::class)
internal class DefaultMultiPaneComponent(
    componentContext: ComponentContext,
) : MultiPaneComponent, ComponentContext by componentContext, DisposableScope by componentContext.disposableScope() {

    private val database = DefaultArticleDatabase()
    private val navigation = PanelsNavigation<Unit, Details, Extra>()
    private val _navState = BehaviorSubject<Panels<Unit, Details, Extra>?>(null)
    private val navState = _navState.notNull()

    override val panels: Value<ChildPanels<*, ArticleListComponent, *, ArticleDetailsComponent, *, ArticleAuthorComponent>> =
        childPanels(
            source = navigation,
            initialPanels = { Panels(main = Unit) },
            serializers = Triple(Unit.serializer(), Details.serializer(), Extra.serializer()),
            onStateChanged = { newState, _ -> _navState.onNext(newState) },
            handleBackButton = true,
            mainFactory = { _, ctx -> listComponent(ctx) },
            detailsFactory = ::detailsComponent,
            extraFactory = ::authorComponent,
        )

    private fun listComponent(componentContext: ComponentContext): ArticleListComponent =
        DefaultArticleListComponent(
            componentContext = componentContext,
            database = database,
            isToolbarVisible = navState.map { it.mode.isSingle },
            selectedArticleId = navState.map { it.takeUnless { it.mode.isSingle }?.details?.articleId },
            onArticleSelected = { articleId, authorId ->
                navigation.navigate { state ->
                    state.copy(
                        details = Details(articleId = articleId, authorId = authorId),
                        extra = if (state.mode == TRIPLE) Extra(authorId = authorId) else null,
                    )
                }
            },
        )

    private fun detailsComponent(config: Details, componentContext: ComponentContext): ArticleDetailsComponent =
        DefaultArticleDetailsComponent(
            componentContext = componentContext,
            database = database,
            articleId = config.articleId,
            isToolbarVisible = navState.map { it.mode.isSingle },
            onAuthorRequested = { navigation.activateExtra(extra = Extra(authorId = config.authorId)) },
            onFinished = navigation::pop,
        )

    private fun authorComponent(config: Extra, componentContext: ComponentContext): ArticleAuthorComponent =
        DefaultArticleAuthorComponent(
            componentContext = componentContext,
            database = database,
            authorId = config.authorId,
            isToolbarVisible = navState.map { it.mode.isSingle },
            isCloseButtonVisible = navState.map { it.mode.isDual },
            onFinished = navigation::pop,
        )

    override fun setMode(mode: ChildPanelsMode) {
        navigation.navigate { state ->
            state.copy(
                extra = state.takeIf { mode == TRIPLE }?.details?.authorId?.let { Extra(authorId = it) } ?: state.extra,
                mode = mode,
            )
        }
    }

    override fun onBack() {
        navigation.pop()
    }

    @Serializable
    private data class Details(val articleId: Long, val authorId: Long)

    @Serializable
    private data class Extra(val authorId: Long)
}
