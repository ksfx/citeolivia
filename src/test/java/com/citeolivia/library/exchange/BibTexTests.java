package com.citeolivia.library.exchange;

import static com.citeolivia.library.Contributor.author;
import static org.assertj.core.api.Assertions.assertThat;

import com.citeolivia.library.ContributorRole;
import com.citeolivia.library.ReferenceDraft;
import com.citeolivia.library.ReferenceType;
import com.citeolivia.library.TestReferences;
import java.util.List;
import org.junit.jupiter.api.Test;

class BibTexTests {

    private static final String SAMPLE = """
            @string{foo = "bar"}
            % comment
            @Article{lecun2015,
              author  = {LeCun, Yann and Yoshua Bengio and Geoffrey Hinton},
              title   = {Deep {L}earning},
              journal = {Nature},
              year    = 2015,
              volume  = {521},
              number  = {7553},
              pages   = {436--444},
              doi     = {10.1038/nature14539}
            }

            @book{destatis2020,
              author = {{Statistisches Bundesamt}},
              title = "M{\\"u}ller und die Gr{\\"o}{\\ss}e",
              publisher = {Destatis},
              address = {Wiesbaden},
              year = {2020}
            }

            @phdthesis{x, author = {Doe, Jane}, title = {Eine Dissertation}, school = {TU München}, year = {2021}}
            @misc{broken, author = {Nobody}}
            """;

    @Test
    void parsesEntries() {
        BibTexImporter.Result result = BibTexImporter.parse(SAMPLE);
        assertThat(result.entries()).hasSize(3);
        assertThat(result.problems()).hasSize(1).first().asString().contains("broken");

        ReferenceDraft article = result.entries().get(0).draft();
        assertThat(result.entries().get(0).key()).isEqualTo("lecun2015");
        assertThat(article.getType()).isEqualTo(ReferenceType.JOURNAL_ARTICLE);
        assertThat(article.getTitle()).isEqualTo("Deep Learning");
        assertThat(article.getContainerTitle()).isEqualTo("Nature");
        assertThat(article.getYear()).isEqualTo("2015");
        assertThat(article.getPages()).isEqualTo("436–444");
        assertThat(article.authors())
                .extracting(c -> c.lastName() + "|" + c.firstName())
                .containsExactly("LeCun|Yann", "Bengio|Yoshua", "Hinton|Geoffrey");

        ReferenceDraft book = result.entries().get(1).draft();
        assertThat(book.getTitle()).isEqualTo("Müller und die Größe");
        assertThat(book.authors()).singleElement().satisfies(c -> {
            assertThat(c.lastName()).isEqualTo("Statistisches Bundesamt");
            assertThat(c.firstName()).isNull();
        });

        ReferenceDraft thesis = result.entries().get(2).draft();
        assertThat(thesis.getType()).isEqualTo(ReferenceType.THESIS);
        assertThat(thesis.getGenre()).isEqualTo("Dissertation");
        assertThat(thesis.getPublisher()).isEqualTo("TU München");
    }

    @Test
    void exportEscapesAndRoundTrips() {
        ReferenceDraft d = TestReferences.draft(
                ReferenceType.JOURNAL_ARTICLE,
                "Kosten & Nutzen: 50% mehr_Effizienz",
                "2023",
                author("Müller", "Anna"),
                author("Statistisches Bundesamt", null));
        d.setContainerTitle("Zeitschrift für Größe");
        d.setPages("12-34");
        String bib = BibTexExporter.export(List.of(TestReferences.of(d, "mueller2023")));

        assertThat(bib)
                .startsWith("@article{mueller2023,")
                .contains("title = {Kosten \\& Nutzen: 50\\% mehr\\_Effizienz}")
                .contains("author = {Müller, Anna and {Statistisches Bundesamt}}")
                .contains("pages = {12--34}");

        ReferenceDraft back = BibTexImporter.parse(bib).entries().get(0).draft();
        assertThat(back.getTitle()).isEqualTo("Kosten & Nutzen: 50% mehr_Effizienz");
        assertThat(back.getContributors()).hasSize(2).allMatch(c -> c.role() == ContributorRole.AUTHOR);
        assertThat(back.getContributors().get(1).lastName()).isEqualTo("Statistisches Bundesamt");
    }

    @Test
    void risExport() {
        ReferenceDraft d = TestReferences.draft(ReferenceType.BOOK, "Titel", "2020", author("Müller", "Anna"));
        d.setPages("1-10");
        String ris = RisExporter.export(List.of(TestReferences.of(d, "m2020")));
        assertThat(ris)
                .contains("TY  - BOOK\r\n", "AU  - Müller, Anna\r\n", "PY  - 2020\r\n", "SP  - 1\r\n", "EP  - 10\r\n")
                .endsWith("ER  - \r\n\r\n");
    }
}
