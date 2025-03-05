package com.accesshr.emsbackend.Entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor

public class Notifications {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
       @Column (name="notificationId")
       int notificationId;

       @Column (name="notificationType")
       String notificationType;

       @Column(name="notification")
       String notification;

       @Column (name="notificationTo")
       String notificationTo;

       @Column (name="isRead")
       Boolean isRead;

}
