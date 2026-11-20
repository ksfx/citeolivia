package com.citeolivia.library.exchange;

import com.citeolivia.library.Contributor;
import com.citeolivia.library.ContributorRole;
import com.citeolivia.library.ReferenceDraft;
import com.citeolivia.library.ReferenceType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tolerant BibTeX/biblatex parser. Unknown entry types become books, {@code @string},
 * {@code @comment} and {@code @preamble} are skipped. Common LaTeX accents are converted to Unicode.
 */
public final class BibTexImporter {

    /** Parsed entry: the draft plus the original key (used as citation key if still free). */
    public record Entry(String key, ReferenceDraft draft) {}

    /** Result: parsed entries and human-readable problems (entries that could not be read). */
    public record Result(List<Entry> entries, List<String> problems) {}

    private static final Map<String, String> ACCENTS = new LinkedHashMap<>();

    static {
        String[][] map = {
            {"\\\"a", "ä"}, {"\\\"o", "ö"}, {"\\\"u", "ü"}, {"\\\"A", "Ä"}, {"\\\"O", "Ö"}, {"\\\"U", "Ü"},
            {"\\ss", "ß"}, {"\\'e", "é"}, {"\\`e", "è"}, {"\\'a", "á"}, {"\\`a", "à"}, {"\\'o", "ó"},
            {"\\'i", "í"}, {"\\'u", "ú"}, {"\\^o", "ô"}, {"\\^e", "ê"}, {"\\c c", "ç"}, {"\\c{c}", "ç"},
            {"\\~n", "ñ"}, {"\\&", "&"}, {"\\%", "%"}, {"\\_", "_"}, {"\\$", "$"}, {"\\#", "#"},
            {"--", "–"}, {"\\textendash", "–"}, {"\\textemdash", "—"}
        };
        for (String[] m : map) {
            ACCENTS.put(m[0], m[1]);
        }
    }

    private final String input;
    private int pos;

    private BibTexImporter(String input) {
        this.input = input;
    }

    public static Result parse(String bibtex) {
        return new BibTexImporter(bibtex == null ? "" : bibtex).parseAll();
    }

    private Result parseAll() {
        List<Entry> entries = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        while (true) {
            int at = input.indexOf('@', pos);
            if (at < 0) {
                break;
            }
            pos = at + 1;
            String type = readWhile(Character::isLetter).toLowerCase(Locale.ROOT);
            skipWhitespace();
            if (pos >= input.length() || (input.charAt(pos) != '{' && input.charAt(pos) != '(')) {
                continue;
            }
            int start = pos;
            int end = matching(pos);
            if (end < 0) {
                problems.add("Eintrag @" + type + " ist nicht vollständig (Klammer fehlt).");
                break;
            }
            String body = input.substring(start + 1, end);
            pos = end + 1;
            if (type.equals("string") || type.equals("comment") || type.equals("preamble")) {
                continue;
            }
            try {
                entries.add(entry(type, body));
            } catch (RuntimeException e) {
                problems.add("Eintrag @" + type + " konnte nicht gelesen werden: " + e.getMessage());
            }
        }
        return new Result(entries, problems);
    }

    private Entry entry(String type, String body) {
        int comma = body.indexOf(',');
        String key = comma < 0 ? body.strip() : body.substring(0, comma).strip();
        Map<String, String> fields = comma < 0 ? Map.of() : fields(body.substring(comma + 1));

        ReferenceDraft d = new ReferenceDraft();
        d.setType(type(type, fields));
        d.setTitle(fields.get("title"));
        d.setSubtitle(fields.get("subtitle"));
        String year = fields.getOrDefault("year", fields.get("date"));
        d.setYear(year == null ? null : year.length() >= 4 ? year.substring(0, 4) : year);
        d.setContainerTitle(first(fields, "journal", "journaltitle", "booktitle", "organization"));
        d.setPublisher(first(fields, "publisher", "school", "institution"));
        d.setPlace(first(fields, "address", "location"));
        d.setEdition(fields.get("edition"));
        d.setSeries(fields.get("series"));
        d.setVolume(fields.get("volume"));
        d.setIssue(first(fields, "number", "issue"));
        d.setPages(fields.get("pages"));
        d.setGenre(fields.get("type"));
        d.setIsbn(fields.get("isbn"));
        d.setDoi(fields.get("doi"));
        d.setUrl(fields.get("url"));
        d.setKeywords(fields.get("keywords"));
        d.setAbstractText(fields.get("abstract"));
        d.setNotes(fields.get("note"));
        String urldate = fields.get("urldate");
        if (urldate != null) {
            try {
                d.setAccessedOn(LocalDate.parse(urldate.strip()));
            } catch (DateTimeParseException ignored) {
                // keep empty
            }
        }
        List<Contributor> people = new ArrayList<>(names(fields.get("author"), ContributorRole.AUTHOR));
        people.addAll(names(fields.get("editor"), ContributorRole.EDITOR));
        d.setContributors(people);
        if (d.getType() == ReferenceType.THESIS && d.getGenre() == null) {
            d.setGenre(type.equals("phdthesis") ? "Dissertation" : "Masterarbeit");
        }
        if (d.getTitle() == null || d.getTitle().isBlank()) {
            throw new IllegalArgumentException("„" + key + "“ hat keinen Titel");
        }
        return new Entry(key, d.normalize());
    }

    private static ReferenceType type(String bibType, Map<String, String> fields) {
        return switch (bibType) {
            case "article" -> ReferenceType.JOURNAL_ARTICLE;
            case "incollection", "inbook" -> ReferenceType.BOOK_CHAPTER;
            case "inproceedings", "conference" -> ReferenceType.CONFERENCE_PAPER;
            case "proceedings", "collection" -> ReferenceType.EDITED_BOOK;
            case "phdthesis", "mastersthesis", "thesis" -> ReferenceType.THESIS;
            case "techreport", "report" -> ReferenceType.REPORT;
            case "online", "electronic", "www" -> ReferenceType.WEB_PAGE;
            case "misc" -> fields.containsKey("url") ? ReferenceType.WEB_PAGE : ReferenceType.BOOK;
            case "book" ->
                !fields.containsKey("author") && fields.containsKey("editor")
                        ? ReferenceType.EDITED_BOOK
                        : ReferenceType.BOOK;
            default -> ReferenceType.BOOK;
        };
    }

    private static String first(Map<String, String> fields, String... keys) {
        for (String k : keys) {
            if (fields.containsKey(k)) {
                return fields.get(k);
            }
        }
        return null;
    }

    /** "Müller, Anna and Carl Meier and {Statistisches Bundesamt}" */
    static List<Contributor> names(String value, ContributorRole role) {
        List<Contributor> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String raw : splitTopLevel(value, " and ")) {
            String n = raw.strip();
            if (n.isEmpty() || n.equalsIgnoreCase("others")) {
                continue;
            }
            if (n.startsWith("{") && n.endsWith("}") && matchingIn(n, 0) == n.length() - 1) {
                result.add(new Contributor(role, clean(n.substring(1, n.length() - 1)), null));
                continue;
            }
            String c = clean(n);
            int comma = c.indexOf(',');
            if (comma > 0) {
                result.add(new Contributor(
                        role,
                        c.substring(0, comma).strip(),
                        c.substring(comma + 1).strip()));
            } else {
                int space = c.lastIndexOf(' ');
                result.add(
                        space > 0
                                ? new Contributor(role, c.substring(space + 1), c.substring(0, space))
                                : new Contributor(role, c, null));
            }
        }
        return result;
    }

    private Map<String, String> fields(String s) {
        Map<String, String> result = new LinkedHashMap<>();
        int i = 0;
        while (i < s.length()) {
            int eq = s.indexOf('=', i);
            if (eq < 0) {
                break;
            }
            String name = s.substring(i, eq).replace(",", "").strip().toLowerCase(Locale.ROOT);
            int v = eq + 1;
            while (v < s.length() && Character.isWhitespace(s.charAt(v))) {
                v++;
            }
            if (v >= s.length()) {
                break;
            }
            String value;
            char c = s.charAt(v);
            if (c == '{') {
                int end = matchingIn(s, v);
                if (end < 0) {
                    throw new IllegalArgumentException("Klammern im Feld „" + name + "“ sind nicht ausgeglichen");
                }
                value = s.substring(v + 1, end);
                i = end + 1;
            } else if (c == '"') {
                int end = v + 1;
                int depth = 0;
                while (end < s.length() && (s.charAt(end) != '"' || depth > 0)) {
                    if (s.charAt(end) == '{') {
                        depth++;
                    } else if (s.charAt(end) == '}') {
                        depth--;
                    }
                    end++;
                }
                value = s.substring(v + 1, Math.min(end, s.length()));
                i = end + 1;
            } else {
                int end = v;
                while (end < s.length() && s.charAt(end) != ',') {
                    end++;
                }
                value = s.substring(v, end).strip();
                i = end + 1;
            }
            if (!name.isEmpty()) {
                result.put(
                        name,
                        name.equals("author") || name.equals("editor") ? value.replaceAll("\\s+", " ") : clean(value));
            }
        }
        return result;
    }

    /** Converts LaTeX markup to plain Unicode text. */
    static String clean(String value) {
        String v = value.replaceAll("\\s+", " ").strip();
        v = v.replaceAll("\\\\([\"'`^~])\\{(\\w)\\}", "\\\\$1$2"); // \"{a} -> \"a
        for (Map.Entry<String, String> e : ACCENTS.entrySet()) {
            v = v.replace("{" + e.getKey() + "}", e.getValue()).replace(e.getKey(), e.getValue());
        }
        v = v.replaceAll("\\\\(?:emph|textit|textbf|textsc|mkbibquote)\\{", "{");
        return v.replace("{", "").replace("}", "").strip();
    }

    private static List<String> splitTopLevel(String s, String separator) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int last = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            } else if (depth == 0 && s.regionMatches(true, i, separator, 0, separator.length())) {
                parts.add(s.substring(last, i));
                last = i + separator.length();
                i = last - 1;
            }
        }
        parts.add(s.substring(last));
        return parts;
    }

    private int matching(int open) {
        return matchingIn(input, open);
    }

    private static int matchingIn(String s, int open) {
        char o = s.charAt(open);
        char close = o == '(' ? ')' : '}';
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == o) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String readWhile(java.util.function.IntPredicate p) {
        int start = pos;
        while (pos < input.length() && p.test(input.charAt(pos))) {
            pos++;
        }
        return input.substring(start, pos);
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
}
