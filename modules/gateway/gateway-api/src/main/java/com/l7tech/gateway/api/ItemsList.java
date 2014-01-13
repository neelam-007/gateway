package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * This represents a reference to a rest entity.
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "List")
@XmlType(name = "ItemsListReferenceType", propOrder = {"name", "id", "type", "date", "links", "content"})
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ItemsList<R> extends Reference<List<Item<R>>> {

    ItemsList() {
    }

    @XmlElement(name = "Item")
    public List<Item<R>> getContent() {
        return super.getContent();
    }

    public void setContent(List<Item<R>> content) {
        super.setContent(content);
    }
}
