package org.dspace.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;

/**
 * SourceToken Entity
 *
 * This table stores authorization tokens for external sources
 * that are allowed to call the Single Upload API.
 *
 * Extends DSpaceObject so UUID column is automatically created
 */
@Entity
@Table(name = "sourcetoken")
public class SourceToken extends DSpaceObject {

    /**
     * Name of the source system calling the API
     */
    @Column(name = "source", nullable = false)
    private String source;

    
    //Token used for API authentication
     
    @Column(name = "token", nullable = false)
    private String token;

    
     //Indicates whether this source is active
     
    @Column(name = "is_active", nullable = false)
    private boolean active;

    
     //Default constructor required by Hibernate
     
    protected SourceToken() {
        super();
    }

    // Getters and Setters

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public String getName() {
        return source;
    }
    
    @Override
    public int getType() {
        return Constants.ITEM;
    }
}