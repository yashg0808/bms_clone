package com.bookmyshow.notification.repository;

import com.bookmyshow.notification.model.Notification;
import com.bookmyshow.notification.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    List<Notification> findByBookingId(UUID bookingId);
}
