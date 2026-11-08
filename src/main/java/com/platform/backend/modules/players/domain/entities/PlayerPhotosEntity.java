package com.platform.backend.modules.players.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * The profile photo of a player, kept in the database itself so the platform needs no paid image storage. A player has
 * at most one; replacing it rewrites the row and removing it deletes the row (the bytes must not linger as a soft
 * delete), so the inherited {@code deleted_*} columns stay unused.
 */
@Entity
@Table(name = "player_photos")
public class PlayerPhotosEntity extends BaseEntity {

    @Column(name = "player_id", nullable = false, unique = true)
    private UUID playerId;

    @Column(name = "content_type", length = 50, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @Column(name = "data", nullable = false)
    private byte[] data;

    public PlayerPhotosEntity() {}

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Integer sizeBytes) { this.sizeBytes = sizeBytes; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
