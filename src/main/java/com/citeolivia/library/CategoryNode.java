package com.citeolivia.library;

/**
 * Category in outline order with its computed number ("2.1") and depth (0 = top level).
 *
 * @param first whether this is the first sibling (cannot move up)
 * @param last whether this is the last sibling (cannot move down)
 */
public record CategoryNode(Long id, Long parentId, String name, String number, int depth, boolean first, boolean last) {

    public String label() {
        return number + " " + name;
    }
}
