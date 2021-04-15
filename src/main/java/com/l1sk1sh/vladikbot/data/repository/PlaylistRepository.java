package com.l1sk1sh.vladikbot.data.repository;

import com.l1sk1sh.vladikbot.data.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Oliver Johnson
 */
@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    Playlist getPlaylistByName(String name);
}