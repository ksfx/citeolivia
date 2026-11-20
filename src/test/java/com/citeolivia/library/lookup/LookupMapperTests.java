package com.citeolivia.library.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.citeolivia.library.ReferenceDraft;
import com.citeolivia.library.ReferenceType;
import org.junit.jupiter.api.Test;

class LookupMapperTests {

    @Test
    void recognisesIdentifiers() {
        assertThat(Identifier.parse("https://doi.org/10.1038/nature14539."))
                .hasValue(new Identifier(Identifier.Kind.DOI, "10.1038/nature14539"));
        assertThat(Identifier.parse("ISBN 978-3-16-148410-0"))
                .hasValue(new Identifier(Identifier.Kind.ISBN, "9783161484100"));
        assertThat(Identifier.parse("0-262-03561-8")).hasValue(new Identifier(Identifier.Kind.ISBN, "0262035618"));
        assertThat(Identifier.parse("978-3-16-148410-1")).isEmpty(); // wrong check digit
        assertThat(Identifier.parse("hallo")).isEmpty();
    }

    @Test
    void crossref() {
        String json = """
                {"status":"ok","message":{"type":"journal-article","title":["Deep learning"],
                 "container-title":["Nature"],"volume":"521","issue":"7553","page":"436-444",
                 "DOI":"10.1038/nature14539","publisher":"Springer",
                 "author":[{"given":"Yann","family":"LeCun"},{"given":"Yoshua","family":"Bengio"}],
                 "issued":{"date-parts":[[2015,5,27]]},"abstract":"<jats:p>Deep learning allows …</jats:p>"}}
                """;
        ReferenceDraft d = LookupMappers.crossref(json).orElseThrow();
        assertThat(d.getType()).isEqualTo(ReferenceType.JOURNAL_ARTICLE);
        assertThat(d.getTitle()).isEqualTo("Deep learning");
        assertThat(d.getContainerTitle()).isEqualTo("Nature");
        assertThat(d.getYear()).isEqualTo("2015");
        assertThat(d.getAbstractText()).isEqualTo("Deep learning allows …");
        assertThat(d.authors()).extracting(c -> c.lastName()).containsExactly("LeCun", "Bengio");
    }

    @Test
    void openLibrary() {
        String json = """
                {"ISBN:9780262035613":{"title":"Deep Learning","publishers":[{"name":"MIT Press"}],
                 "publish_date":"3 January 2017","authors":[{"name":"Ian Goodfellow"},{"name":"Yoshua Bengio"}]}}
                """;
        ReferenceDraft d = LookupMappers.openLibrary(json, "9780262035613").orElseThrow();
        assertThat(d.getType()).isEqualTo(ReferenceType.BOOK);
        assertThat(d.getYear()).isEqualTo("2017");
        assertThat(d.getPublisher()).isEqualTo("MIT Press");
        assertThat(d.getIsbn()).isEqualTo("9780262035613");
        assertThat(d.authors())
                .extracting(c -> c.lastName() + ", " + c.firstName())
                .containsExactly("Goodfellow, Ian", "Bengio, Yoshua");
        assertThat(LookupMappers.openLibrary("{}", "9780262035613")).isEmpty();
    }

    @Test
    void dnb() {
        String xml = """
                <searchRetrieveResponse xmlns="http://www.loc.gov/zing/srw/"><records><record><recordData>
                <dc xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <dc:title>Wissenschaftliches Arbeiten : Ein Leitfaden / Anna Müller</dc:title>
                  <dc:creator>Müller, Anna [Verfasser]</dc:creator>
                  <dc:publisher>Wiesbaden : Springer Gabler</dc:publisher>
                  <dc:date>2022</dc:date>
                </dc></recordData></record></records></searchRetrieveResponse>
                """;
        ReferenceDraft d = LookupMappers.dnb(xml, "9783658356010").orElseThrow();
        assertThat(d.getTitle()).isEqualTo("Wissenschaftliches Arbeiten");
        assertThat(d.getSubtitle()).isEqualTo("Ein Leitfaden");
        assertThat(d.getPlace()).isEqualTo("Wiesbaden");
        assertThat(d.getPublisher()).isEqualTo("Springer Gabler");
        assertThat(d.authors())
                .singleElement()
                .satisfies(c -> assertThat(c.displayName()).isEqualTo("Müller, Anna"));
    }

    @Test
    void dnbRejectsDoctype() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>";
        assertThatThrownBy(() -> LookupMappers.dnb(xxe, "1")).isInstanceOf(IllegalStateException.class);
    }
}
