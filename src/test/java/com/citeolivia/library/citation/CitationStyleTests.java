package com.citeolivia.library.citation;

import static com.citeolivia.library.Contributor.author;
import static com.citeolivia.library.Contributor.editor;
import static org.assertj.core.api.Assertions.assertThat;

import com.citeolivia.library.Reference;
import com.citeolivia.library.ReferenceDraft;
import com.citeolivia.library.ReferenceType;
import com.citeolivia.library.TestReferences;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CitationStyleTests {

    private static Reference book() {
        ReferenceDraft d = TestReferences.draft(
                ReferenceType.BOOK,
                "Deep Learning",
                "2016",
                author("Goodfellow", "Ian"),
                author("Bengio", "Yoshua"),
                author("Courville", "Aaron"));
        d.setSubtitle("Grundlagen");
        d.setPublisher("MIT Press");
        d.setPlace("Cambridge");
        d.setEdition("2");
        d.setIsbn("978-0262035613");
        return TestReferences.of(d, "goodfellow2016");
    }

    private static Reference article() {
        ReferenceDraft d = TestReferences.draft(
                ReferenceType.JOURNAL_ARTICLE,
                "Deep learning",
                "2015",
                author("LeCun", "Yann"),
                author("Bengio", "Yoshua"));
        d.setContainerTitle("Nature");
        d.setVolume("521");
        d.setIssue("7553");
        d.setPages("436-444");
        d.setDoi("10.1038/nature14539");
        return TestReferences.of(d, "lecun2015");
    }

    private static Reference chapter() {
        ReferenceDraft d = TestReferences.draft(
                ReferenceType.BOOK_CHAPTER,
                "Qualitative Inhaltsanalyse",
                "2019",
                author("Mayring", "Philipp"),
                editor("Baur", "Nina"),
                editor("Blasius", "Jörg"));
        d.setContainerTitle("Handbuch Methoden der empirischen Sozialforschung");
        d.setPublisher("Springer VS");
        d.setPlace("Wiesbaden");
        d.setEdition("2");
        d.setPages("633-648");
        return TestReferences.of(d, "mayring2019");
    }

    @Test
    void apaBook() {
        assertThat(CitationStyle.APA7.bibliography(book()).plain())
                .isEqualTo(
                        "Goodfellow, I., Bengio, Y., & Courville, A. (2016). Deep Learning: Grundlagen (2. Aufl.). MIT Press.");
        assertThat(CitationStyle.APA7.bibliography(book()).html()).contains("<i>Deep Learning: Grundlagen</i>");
    }

    @Test
    void apaJournalArticle() {
        assertThat(CitationStyle.APA7.bibliography(article()).plain())
                .isEqualTo(
                        "LeCun, Y., & Bengio, Y. (2015). Deep learning. Nature, 521(7553), 436–444. https://doi.org/10.1038/nature14539");
        assertThat(CitationStyle.APA7.bibliography(article()).html()).contains("<i>Nature</i>, <i>521</i>(7553)");
    }

    @Test
    void apaChapter() {
        assertThat(CitationStyle.APA7.bibliography(chapter()).plain())
                .isEqualTo("Mayring, P. (2019). Qualitative Inhaltsanalyse. In N. Baur & J. Blasius (Hrsg.), "
                        + "Handbuch Methoden der empirischen Sozialforschung (2. Aufl., S. 633–648). Springer VS.");
    }

    @Test
    void apaInText() {
        assertThat(CitationStyle.APA7.inText(book(), "12-14")).isEqualTo("(Goodfellow et al., 2016, S. 12–14)");
        assertThat(CitationStyle.APA7.inText(article(), null)).isEqualTo("(LeCun & Bengio, 2015)");
    }

    @Test
    void apaWebPageWithoutYear() {
        ReferenceDraft d = TestReferences.draft(
                ReferenceType.WEB_PAGE, "Bevölkerungsstand", null, author("Statistisches Bundesamt", null));
        d.setContainerTitle("Destatis");
        d.setUrl("https://www.destatis.de/bevoelkerung");
        d.setAccessedOn(LocalDate.of(2024, 5, 3));
        assertThat(CitationStyle.APA7.bibliography(TestReferences.of(d, "x")).plain())
                .isEqualTo("Statistisches Bundesamt. (o. J.). Bevölkerungsstand. Destatis. "
                        + "Abgerufen am 3. Mai 2024, von https://www.destatis.de/bevoelkerung");
    }

    @Test
    void dinBookArticleChapter() {
        assertThat(CitationStyle.DIN_ISO_690.bibliography(book()).plain())
                .isEqualTo("Goodfellow, Ian; Bengio, Yoshua; Courville, Aaron: Deep Learning: Grundlagen. 2. Aufl. "
                        + "Cambridge: MIT Press, 2016. ISBN 978-0262035613.");
        assertThat(CitationStyle.DIN_ISO_690.bibliography(article()).plain())
                .isEqualTo("LeCun, Yann; Bengio, Yoshua: Deep learning. In: Nature 521 (2015), Nr. 7553, S. 436–444. "
                        + "DOI: 10.1038/nature14539.");
        assertThat(CitationStyle.DIN_ISO_690.bibliography(chapter()).plain())
                .isEqualTo(
                        "Mayring, Philipp: Qualitative Inhaltsanalyse. In: Baur, Nina; Blasius, Jörg (Hrsg.): "
                                + "Handbuch Methoden der empirischen Sozialforschung. 2. Aufl. Wiesbaden: Springer VS, 2019, S. 633–648.");
        assertThat(CitationStyle.DIN_ISO_690.inText(book(), "5")).isEqualTo("(Goodfellow u. a. 2016, S. 5)");
    }

    @Test
    void htmlIsEscaped() {
        ReferenceDraft d =
                TestReferences.draft(ReferenceType.BOOK, "<script>alert(1)</script>", "2020", author("A&B", "C"));
        String html = CitationStyle.APA7.bibliography(TestReferences.of(d, "x")).html();
        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;").contains("A&amp;B");
    }
}
