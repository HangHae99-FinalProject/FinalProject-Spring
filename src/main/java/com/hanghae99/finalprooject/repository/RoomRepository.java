package com.hanghae99.finalprooject.repository;

import com.hanghae99.finalprooject.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room,Long> {

    List<Room> findByRoomPostId(Long post);
    Optional<Room> findByRoomNameAndRoomPostId(String roomName, Long postId);
    Optional<Room> findByRoomName(String roomName);
}