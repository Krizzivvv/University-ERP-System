-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: univ_erp
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `univ_erp`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `univ_erp` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `univ_erp`;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `course_id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `credits` int NOT NULL,
  PRIMARY KEY (`course_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES (1,'CSE101','Intro to Programming',4),(7,'MTH100','Linear Algebra',4),(8,'DES102','HCI',4),(9,'COM101','Communication Skills',4),(10,'ECE111','Digital Circuit',4),(11,'MTH-201','Probability and Statistics',4),(17,'CSE201','DSA',4),(18,'CSE112','CO',4),(19,'DES101','DDV',4),(21,'DES100','VDC',4);
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollments`
--

DROP TABLE IF EXISTS `enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollments` (
  `enrollment_id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `section_id` int NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'enrolled',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `enrolled_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uniq_student_section` (`student_id`,`section_id`),
  KEY `fk_enroll_section` (`section_id`),
  KEY `idx_enrollments_student_status` (`student_id`,`status`),
  CONSTRAINT `fk_enroll_section` FOREIGN KEY (`section_id`) REFERENCES `sections` (`section_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_enroll_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollments`
--

LOCK TABLES `enrollments` WRITE;
/*!40000 ALTER TABLE `enrollments` DISABLE KEYS */;
INSERT INTO `enrollments` VALUES (70,3,26,'enrolled','2025-11-22 00:48:17','2025-11-21 19:18:17'),(72,3,30,'enrolled','2025-11-22 00:48:33','2025-11-21 19:18:33'),(82,111,28,'enrolled','2025-11-22 03:28:03','2025-11-21 21:58:03'),(83,88,31,'enrolled','2025-11-22 03:28:32','2025-11-21 21:58:32'),(85,88,28,'enrolled','2025-11-22 03:29:10','2025-11-21 21:59:10'),(87,88,29,'enrolled','2025-11-22 03:40:28','2025-11-21 22:10:28'),(88,3,31,'enrolled','2025-11-22 21:51:48','2025-11-22 16:21:48'),(89,3,28,'enrolled','2025-11-22 21:51:56','2025-11-22 16:21:56'),(90,3,25,'enrolled','2025-11-22 21:52:03','2025-11-22 16:22:03'),(91,3,29,'enrolled','2025-11-26 15:37:59','2025-11-26 10:07:59'),(92,115,25,'enrolled','2025-11-26 17:56:31','2025-11-26 12:26:31'),(93,115,26,'enrolled','2025-11-26 17:58:16','2025-11-26 12:28:16'),(94,115,28,'enrolled','2025-11-26 17:58:24','2025-11-26 12:28:24'),(95,115,30,'enrolled','2025-11-26 17:58:34','2025-11-26 12:28:34'),(96,115,31,'enrolled','2025-11-26 17:58:40','2025-11-26 12:28:40');
/*!40000 ALTER TABLE `enrollments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `evaluation_components`
--

DROP TABLE IF EXISTS `evaluation_components`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_components` (
  `component_id` int NOT NULL AUTO_INCREMENT,
  `section_id` int NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight` decimal(5,2) DEFAULT '0.00',
  `max_score` decimal(7,2) DEFAULT '100.00',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`component_id`),
  UNIQUE KEY `uq_section_name` (`section_id`,`name`),
  CONSTRAINT `evaluation_components_ibfk_1` FOREIGN KEY (`section_id`) REFERENCES `sections` (`section_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evaluation_components`
--

LOCK TABLES `evaluation_components` WRITE;
/*!40000 ALTER TABLE `evaluation_components` DISABLE KEYS */;
INSERT INTO `evaluation_components` VALUES (6,29,'Quiz',20.00,36.00,'2025-11-25 13:57:03'),(7,29,'Midsem',35.00,100.00,'2025-11-25 22:46:20'),(8,29,'Endsem',40.00,100.00,'2025-11-25 22:46:57'),(9,29,'assignment',5.00,100.00,'2025-11-25 22:47:42'),(10,28,'midsem',20.00,100.00,'2025-11-26 12:03:33'),(11,28,'endsem',35.00,100.00,'2025-11-26 12:03:47'),(15,31,'midsem',20.00,100.00,'2025-11-26 12:11:49'),(16,31,'endsem',40.00,100.00,'2025-11-26 12:12:07'),(45,31,'Assignment',20.00,10.00,'2025-11-26 14:19:10'),(46,31,'Quiz',20.00,10.00,'2025-11-26 14:19:10');
/*!40000 ALTER TABLE `evaluation_components` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grades`
--

DROP TABLE IF EXISTS `grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grades` (
  `grade_id` int NOT NULL AUTO_INCREMENT,
  `enrollment_id` int NOT NULL,
  `component` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` decimal(6,2) DEFAULT NULL,
  `final_grade` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`grade_id`),
  UNIQUE KEY `unique_enrollment_component` (`enrollment_id`,`component`),
  CONSTRAINT `fk_grade_enroll` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`enrollment_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=193 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grades`
--

LOCK TABLES `grades` WRITE;
/*!40000 ALTER TABLE `grades` DISABLE KEYS */;
INSERT INTO `grades` VALUES (52,87,'Quiz',32.00,NULL),(54,87,'Midsem',47.00,NULL),(55,87,'Endsem',79.00,NULL),(56,87,'assignment',79.00,NULL),(73,87,'FINAL_LETTER',NULL,'D'),(79,87,'FINAL_TOTAL',58.40,NULL),(87,91,'Quiz',32.00,NULL),(88,91,'Midsem',79.00,NULL),(89,91,'Endsem',54.00,NULL),(90,91,'assignment',89.00,NULL),(91,91,'FINAL_TOTAL',60.10,NULL),(92,91,'FINAL_LETTER',NULL,'C'),(93,85,'midsem',100.00,NULL),(94,85,'endsem',75.00,NULL),(97,85,'assignment',100.00,NULL),(113,83,'midsem',60.00,NULL),(114,83,'endsem',95.00,NULL),(116,83,'quiz',9.00,NULL),(131,92,'assignment',95.00,NULL),(132,92,'quiz',28.00,NULL),(134,92,'midsem',10.00,NULL),(135,92,'endsem',9.00,NULL),(137,93,'midsem',8.00,NULL),(138,93,'endsem',9.50,NULL),(141,93,'quiz',30.00,NULL),(149,96,'midsem',68.00,NULL),(150,96,'endsem',75.00,NULL),(152,96,'quiz',40.00,NULL),(157,95,'Quiz',20.00,NULL),(158,95,'Midsem',62.00,NULL),(159,95,'Endsem',55.00,NULL),(160,95,'assignment',100.00,NULL),(189,83,'Assignment',9.00,NULL),(191,83,'FINAL_TOTAL',51.80,NULL),(192,83,'FINAL_LETTER',NULL,'D');
/*!40000 ALTER TABLE `grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instructors`
--

DROP TABLE IF EXISTS `instructors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `instructors` (
  `user_id` int NOT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_instructor_user` FOREIGN KEY (`user_id`) REFERENCES `univ_auth`.`users_auth` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instructors`
--

LOCK TABLES `instructors` WRITE;
/*!40000 ALTER TABLE `instructors` DISABLE KEYS */;
INSERT INTO `instructors` VALUES (2,'Computer Science'),(82,'General'),(83,'General'),(84,'General'),(85,'General'),(86,'General'),(91,'General'),(92,'General'),(93,'General'),(94,'General'),(96,'General'),(97,'General'),(98,'General'),(99,'General'),(100,'General');
/*!40000 ALTER TABLE `instructors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sections`
--

DROP TABLE IF EXISTS `sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sections` (
  `section_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `instructor_id` int DEFAULT NULL,
  `day_time` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `room` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `capacity` int NOT NULL DEFAULT '30',
  `semester` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `year` int DEFAULT NULL,
  `registration_start` timestamp NULL DEFAULT NULL,
  `registration_end` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`section_id`),
  KEY `fk_section_course` (`course_id`),
  KEY `idx_sections_instructor` (`instructor_id`),
  CONSTRAINT `fk_section_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_section_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `instructors` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sections`
--

LOCK TABLES `sections` WRITE;
/*!40000 ALTER TABLE `sections` DISABLE KEYS */;
INSERT INTO `sections` VALUES (25,19,91,'Tue 11:00-12:30','B-102',30,'Winter',2025,NULL,NULL),(26,21,91,'Wed 11:00-12:30','B-102',60,'Winter',2025,NULL,NULL),(28,17,100,'Mon 9:30-11','C-201',300,'Winter',2025,NULL,NULL),(29,11,92,'Mon 15:00-16:30','C-201',30,'winter',2025,NULL,NULL),(30,11,93,'mon 15:00-16:30','C-101',30,'winter',2025,NULL,NULL),(31,18,94,'Fri 9:30-12:30','C-102',30,'Winter',2025,NULL,NULL);
/*!40000 ALTER TABLE `sections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `settings`
--

DROP TABLE IF EXISTS `settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `settings` (
  `key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `settings`
--

LOCK TABLES `settings` WRITE;
/*!40000 ALTER TABLE `settings` DISABLE KEYS */;
INSERT INTO `settings` VALUES ('drop_deadline_days','30'),('maintenance_on','false');
/*!40000 ALTER TABLE `settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `user_id` int NOT NULL,
  `roll_no` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `program` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `year` int DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `roll_no` (`roll_no`),
  CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `univ_auth`.`users_auth` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (3,'2021CS101','B.Tech CS',3),(49,'R49','B.Tech CSE',1),(50,'R50','B.Tech CSE',1),(51,'R51','B.Tech CSE',1),(52,'R52','B.Tech CSE',1),(53,'R53','B.Tech CSE',1),(54,'R54','B.Tech CSE',1),(55,'R55','B.Tech CSE',1),(56,'R56','B.Tech CSE',1),(57,'R57','B.Tech CSE',1),(58,'R58','B.Tech CSE',1),(59,'R59','B.Tech CSE',1),(60,'R60','B.Tech CSE',1),(61,'R61','B.Tech CSE',1),(62,'R62','B.Tech CSE',1),(63,'R63','B.Tech CSE',1),(64,'R64','B.Tech CSE',1),(65,'R65','B.Tech CSE',1),(66,'R66','B.Tech CSE',1),(67,'R67','B.Tech CSE',1),(68,'R68','B.Tech CSE',1),(69,'R69','B.Tech CSE',1),(70,'R70','B.Tech CSE',1),(71,'R71','B.Tech CSE',1),(72,'R72','B.Tech CSE',1),(73,'R73','B.Tech CSE',1),(74,'R74','B.Tech CSE',1),(75,'R75','B.Tech CSE',1),(76,'R76','B.Tech CSE',1),(77,'R77','B.Tech CSE',1),(87,'R87','B.Tech CSE',1),(88,'R88','B.Tech CSE',1),(101,'R101','B.Tech CSE',1),(102,'R102','B.Tech CSE',1),(111,'2024308','betch',2),(112,'2024042','btech',2),(115,'2024050','CSD',2);
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Current Database: `univ_auth`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `univ_auth` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `univ_auth`;

--
-- Table structure for table `users_auth`
--

DROP TABLE IF EXISTS `users_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_auth` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('admin','instructor','student') COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'active',
  `last_login` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_auth`
--

LOCK TABLES `users_auth` WRITE;
/*!40000 ALTER TABLE `users_auth` DISABLE KEYS */;
INSERT INTO `users_auth` VALUES (2,'inst1','instructor','$2a$10$/FFSLi5M1lpPT0vJqn85tue.NKrR04zyH2te4ML/c8ycHcmJLt2wy','active','2025-11-26 03:57:20'),(3,'stu1','student','$2a$12$OmkDeSq4lFCZ5PSLZvuIpONBCDFXdlyHVqVfTOyzOs4QypxUJWaDW','active','2025-11-26 15:45:03'),(8,'admin','admin','$2a$12$9g2NZMEB6aRZU2VzmWdWv.JlXjEjjYUPXGYOHrdS89SpvTDn6ah1O','active','2025-11-27 01:31:44'),(49,'student1','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(50,'student2','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(51,'student3','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(52,'student4','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(53,'student5','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(54,'student6','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(55,'student7','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(56,'student8','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(57,'student9','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(58,'student10','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(59,'student11','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(60,'student12','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(61,'student13','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(62,'student14','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(63,'student15','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(64,'student16','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(65,'student17','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(66,'student18','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(67,'student19','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(68,'student20','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(69,'student21','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(70,'student22','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(71,'student23','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(72,'student24','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(73,'student25','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(74,'student26','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(75,'student27','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(76,'student28','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(77,'student29','student','$2a$10$JjM1MbdFKHkY1qZ5d2V7CeVckfTHogBeGSvPa7hHGHkE/preRG.ZG','active',NULL),(82,'pankaj','instructor','$2a$10$87K8Hg7TbmP5VqhfNKFSF.x21UnjSsWpCqHOdIv7cJ5AFFHxU.00i','active',NULL),(83,'subhajit','instructor','$2a$10$n6CuTcgo47Qljy3emLmzWOESSoOiPCx.urKL6hQMnlw8zyPvRRKVW','active',NULL),(84,'sonal','instructor','$2a$10$gZMG.5HP.roroeRbt1x1quelOKN3vYo01Lv3NikC6xqK6OwuhShje','active',NULL),(85,'pravesh','instructor','$2a$10$SLCUTFd4Ykfm9iwA4rIgveo3ALlNL6pJHXstepQ0OHuZg1sXzdurK','active',NULL),(86,'payel','instructor','$2a$10$pfBeJY2vQdqbZMCqyeS63en8DFGViH8cUwHktjQ9a1uOuskQaYrQC','active','2025-11-19 23:49:15'),(87,'aish','student','$2a$10$nallNFX4L8eTTG.ZnmciIetIiN15waZi/OqgiLYWfopGDntfmPFUu','active','2025-11-22 00:43:30'),(88,'krishiv','student','$2a$10$rxkUQQDO/V2Wm2nnFqKTY.1kbQ6UYWCnzioprTnrXIzRI9puaitjq','active','2025-11-27 00:46:07'),(91,'anoop','instructor','$2a$10$2w6DY/g7xZI3qiEw4c83pOBb3v2n/WHmX2Sy6NWiATwGIyhKYbUgi','active','2025-11-26 20:46:28'),(92,'sanjit','instructor','$2a$10$WfXK6nuIfjOWzK02SlLH0OIrKOtoLyf80Q5DJFWKbfPXHjF9XD4Oe','active','2025-11-27 00:45:31'),(93,'sneha','instructor','$2a$10$hBX..qJI4GDMLQgzrJWasuE3IrFQHmamzot1nJpg9LLvulvGjZ52O','active','2025-11-26 18:16:55'),(94,'sujay','instructor','$2a$10$NZ.cI1HutnTrMnD.xYQ4beqobvuAH03f.WDymxODA7Asm0j.qhebS','active','2025-11-26 19:49:05'),(96,'prahllad','instructor','$2a$10$BfyZ9isekWxKNMtkLNS4YuOmefZovpw1q8ptRim.hvXLyAm9Lqlbe','active',NULL),(97,'ojaswa','instructor','$2a$10$dHBESYhYz4KEUOfhRN4mbese7jxB1Pm54LVX7vtcQ2SP.kylrUR7.','active','2025-11-26 17:32:11'),(98,'tamam','instructor','$2a$10$MaFIjLIorJ6GevZAYxk9Ee6OmDLPYOo/d4wmKCNon.Uy3M1oxwRvG','active',NULL),(99,'shad','instructor','$2a$10$5yvuj2iAZTSgcs96ywrn.OJRgty8ApuXgIwi.bN/SMfhsbgPlV3dq','active',NULL),(100,'Gautam','instructor','$2a$10$PPfHZiFaHzYzySoIdBA2e.nSy33zazeBdviZFxZYj8eG77PCbOeIe','active','2025-11-26 17:48:23'),(101,'krishivvats','student','$2a$10$sTXz0.V6Z4QoVUtk3c8B5uLGTvHjdJOiQ9RS3..ksRmHYrEOeXAEO','active','2025-11-22 01:08:47'),(102,'nishank','student','$2a$10$IQmPMIoj/ievzRDsX1JDW.1iGxZYNIZp2e1jGc5kZ1YA0u6qLBgKe','active','2025-11-22 02:10:04'),(103,'shravy','student','$2a$10$wTsAPvPYgMeH.5HcFk.uBerfMw/QSjP84jDCEjiGBnAlIqoA0EuBu','active',NULL),(111,'oppa','student','$2a$10$MGswJ9grm4IVz1Z6rO.SMulT7bnPDqQO6Ta/iKRvZsdVjt61f0hLG','active','2025-11-22 03:28:00'),(112,'advit','student','$2a$10$YbqLulNOFs/SxB3ezXglWOMw0Lp3DvUJ713c3elrxVGpb/OED.W5a','active','2025-11-22 03:36:52'),(115,'aishwary','student','$2a$10$L/mAy2ohrsDkHJGDdk7nT.yTNVcjqs3Y4sOFpShUnloS/yKocaIHS','active','2025-11-27 01:31:20');
/*!40000 ALTER TABLE `users_auth` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-27  1:31:59
