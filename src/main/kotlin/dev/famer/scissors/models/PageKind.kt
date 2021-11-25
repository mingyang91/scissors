package dev.famer.scissors.models

import org.apache.pdfbox.pdmodel.PDPage

sealed class PageKind {
    abstract val index: Int
    abstract val page: PDPage

    data class Cover(override val index: Int,
                     override val page: PDPage,
                     val spans: List<Span>): PageKind()

    data class Content(override val index: Int,
                       override val page: PDPage): PageKind()

    data class HandWrite(override val index: Int,
                         override val page: PDPage): PageKind()

}

