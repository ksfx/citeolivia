package com.citeolivia.library;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Editable data of a reference – input for creating/updating (from forms, DOI/ISBN lookups or
 * BibTeX import). Mutable so that it can be bound by Spring MVC.
 */
public class ReferenceDraft {

    private ReferenceType type;

    @NotBlank(message = "Bitte einen Titel angeben")
    private String title;

    private String subtitle;

    @Size(max = 20, message = "Maximal 20 Zeichen")
    private String year;

    private String containerTitle;

    private String publisher;

    private String place;

    private String edition;

    private String series;

    private String volume;

    private String issue;

    private String pages;

    private String genre;

    private String isbn;

    private String doi;

    private String url;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate accessedOn;

    private String abstractText;

    private String keywords;

    private String notes;

    private List<Contributor> contributors = new ArrayList<>();

    /** Trims all text fields and turns blank values into {@code null}. */
    public ReferenceDraft normalize() {

        title = clean(title);
        subtitle = clean(subtitle);
        year = clean(year);
        containerTitle = clean(containerTitle);
        publisher = clean(publisher);
        place = clean(place);
        edition = clean(edition);
        series = clean(series);
        volume = clean(volume);
        issue = clean(issue);
        pages = clean(pages);
        genre = clean(genre);
        isbn = clean(isbn);
        doi = clean(doi);
        url = clean(url);
        abstractText = clean(abstractText);
        keywords = clean(keywords);
        notes = clean(notes);
        contributors = contributors.stream()
                .filter(c -> c.lastName() != null && !c.lastName().isBlank())
                .map(c -> new Contributor(c.role(), c.lastName().strip(), clean(c.firstName())))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return this;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String s = value.strip();
        return s.isEmpty() ? null : s;
    }

    public List<Contributor> authors() {
        return contributors.stream()
                .filter(c -> c.role() == ContributorRole.AUTHOR)
                .toList();
    }

    public List<Contributor> editors() {
        return contributors.stream()
                .filter(c -> c.role() == ContributorRole.EDITOR)
                .toList();
    }

    public ReferenceType getType() {
        return type;
    }

    public void setType(ReferenceType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getContainerTitle() {
        return containerTitle;
    }

    public void setContainerTitle(String containerTitle) {
        this.containerTitle = containerTitle;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDate getAccessedOn() {
        return accessedOn;
    }

    public void setAccessedOn(LocalDate accessedOn) {
        this.accessedOn = accessedOn;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }
}
