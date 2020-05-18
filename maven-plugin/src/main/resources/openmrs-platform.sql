-- MySQL dump 10.13  Distrib 5.6.30, for debian-linux-gnu (x86_64)
-- ------------------------------------------------------
-- Server version	5.6.30-0ubuntu0.14.04.1
--
-- Dumped from OpenMRS Platform 1.9.8

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `active_list`
--

DROP TABLE IF EXISTS `active_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `active_list` (
  `active_list_id` int(11) NOT NULL AUTO_INCREMENT,
  `active_list_type_id` int(11) NOT NULL,
  `person_id` int(11) NOT NULL,
  `concept_id` int(11) NOT NULL,
  `start_obs_id` int(11) DEFAULT NULL,
  `stop_obs_id` int(11) DEFAULT NULL,
  `start_date` datetime NOT NULL,
  `end_date` datetime DEFAULT NULL,
  `comments` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`active_list_id`),
  KEY `user_who_voided_active_list` (`voided_by`),
  KEY `user_who_created_active_list` (`creator`),
  KEY `active_list_type_of_active_list` (`active_list_type_id`),
  KEY `person_of_active_list` (`person_id`),
  KEY `concept_active_list` (`concept_id`),
  KEY `start_obs_active_list` (`start_obs_id`),
  KEY `stop_obs_active_list` (`stop_obs_id`),
  CONSTRAINT `active_list_type_of_active_list` FOREIGN KEY (`active_list_type_id`) REFERENCES `active_list_type` (`active_list_type_id`),
  CONSTRAINT `concept_active_list` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `person_of_active_list` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`),
  CONSTRAINT `start_obs_active_list` FOREIGN KEY (`start_obs_id`) REFERENCES `obs` (`obs_id`),
  CONSTRAINT `stop_obs_active_list` FOREIGN KEY (`stop_obs_id`) REFERENCES `obs` (`obs_id`),
  CONSTRAINT `user_who_created_active_list` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_active_list` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `active_list`
--

LOCK TABLES `active_list` WRITE;
/*!40000 ALTER TABLE `active_list` DISABLE KEYS */;
/*!40000 ALTER TABLE `active_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `active_list_allergy`
--

DROP TABLE IF EXISTS `active_list_allergy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `active_list_allergy` (
  `active_list_id` int(11) NOT NULL AUTO_INCREMENT,
  `allergy_type` varchar(50) DEFAULT NULL,
  `reaction_concept_id` int(11) DEFAULT NULL,
  `severity` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`active_list_id`),
  KEY `reaction_allergy` (`reaction_concept_id`),
  CONSTRAINT `reaction_allergy` FOREIGN KEY (`reaction_concept_id`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `active_list_allergy`
--

LOCK TABLES `active_list_allergy` WRITE;
/*!40000 ALTER TABLE `active_list_allergy` DISABLE KEYS */;
/*!40000 ALTER TABLE `active_list_allergy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `active_list_problem`
--

DROP TABLE IF EXISTS `active_list_problem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `active_list_problem` (
  `active_list_id` int(11) NOT NULL AUTO_INCREMENT,
  `status` varchar(50) DEFAULT NULL,
  `sort_weight` double DEFAULT NULL,
  PRIMARY KEY (`active_list_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `active_list_problem`
--

LOCK TABLES `active_list_problem` WRITE;
/*!40000 ALTER TABLE `active_list_problem` DISABLE KEYS */;
/*!40000 ALTER TABLE `active_list_problem` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `active_list_type`
--

DROP TABLE IF EXISTS `active_list_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `active_list_type` (
  `active_list_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`active_list_type_id`),
  KEY `user_who_retired_active_list_type` (`retired_by`),
  KEY `user_who_created_active_list_type` (`creator`),
  CONSTRAINT `user_who_created_active_list_type` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_active_list_type` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `active_list_type`
--

LOCK TABLES `active_list_type` WRITE;
/*!40000 ALTER TABLE `active_list_type` DISABLE KEYS */;
INSERT INTO `active_list_type` VALUES (1,'Allergy','An Allergy the Patient may have',1,'2010-05-28 00:00:00',0,NULL,NULL,NULL,'96f4f603-6a99-11df-a648-37a07f9c90fb'),(2,'Problem','A Problem the Patient may have',1,'2010-05-28 00:00:00',0,NULL,NULL,NULL,'a0c7422b-6a99-11df-a648-37a07f9c90fb');
/*!40000 ALTER TABLE `active_list_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `clob_datatype_storage`
--

DROP TABLE IF EXISTS `clob_datatype_storage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `clob_datatype_storage` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` char(38) NOT NULL,
  `value` longtext NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `clob_datatype_storage_uuid_index` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `clob_datatype_storage`
--

LOCK TABLES `clob_datatype_storage` WRITE;
/*!40000 ALTER TABLE `clob_datatype_storage` DISABLE KEYS */;
/*!40000 ALTER TABLE `clob_datatype_storage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cohort`
--

DROP TABLE IF EXISTS `cohort`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cohort` (
  `cohort_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`cohort_id`),
  UNIQUE KEY `cohort_uuid_index` (`uuid`),
  KEY `user_who_changed_cohort` (`changed_by`),
  KEY `cohort_creator` (`creator`),
  KEY `user_who_voided_cohort` (`voided_by`),
  CONSTRAINT `cohort_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_changed_cohort` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_cohort` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cohort`
--

LOCK TABLES `cohort` WRITE;
/*!40000 ALTER TABLE `cohort` DISABLE KEYS */;
/*!40000 ALTER TABLE `cohort` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cohort_member`
--

DROP TABLE IF EXISTS `cohort_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cohort_member` (
  `cohort_id` int(11) NOT NULL DEFAULT '0',
  `patient_id` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`cohort_id`,`patient_id`),
  KEY `member_patient` (`patient_id`),
  CONSTRAINT `member_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON UPDATE CASCADE,
  CONSTRAINT `parent_cohort` FOREIGN KEY (`cohort_id`) REFERENCES `cohort` (`cohort_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cohort_member`
--

LOCK TABLES `cohort_member` WRITE;
/*!40000 ALTER TABLE `cohort_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `cohort_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept`
--

DROP TABLE IF EXISTS `concept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept` (
  `concept_id` int(11) NOT NULL AUTO_INCREMENT,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `short_name` varchar(255) DEFAULT NULL,
  `description` text,
  `form_text` text,
  `datatype_id` int(11) NOT NULL DEFAULT '0',
  `class_id` int(11) NOT NULL DEFAULT '0',
  `is_set` tinyint(1) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_id`),
  UNIQUE KEY `concept_uuid_index` (`uuid`),
  KEY `user_who_changed_concept` (`changed_by`),
  KEY `concept_classes` (`class_id`),
  KEY `concept_creator` (`creator`),
  KEY `concept_datatypes` (`datatype_id`),
  KEY `user_who_retired_concept` (`retired_by`),
  CONSTRAINT `concept_classes` FOREIGN KEY (`class_id`) REFERENCES `concept_class` (`concept_class_id`),
  CONSTRAINT `concept_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `concept_datatypes` FOREIGN KEY (`datatype_id`) REFERENCES `concept_datatype` (`concept_datatype_id`),
  CONSTRAINT `user_who_changed_concept` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_concept` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept`
--

LOCK TABLES `concept` WRITE;
/*!40000 ALTER TABLE `concept` DISABLE KEYS */;
INSERT INTO `concept` VALUES (1,0,'','',NULL,4,11,0,1,'2016-07-07 08:14:50',NULL,NULL,NULL,NULL,NULL,NULL,'d57ee217-fabe-4577-bf8f-285fb6120e21'),(2,0,'','',NULL,4,11,0,1,'2016-07-07 08:14:50',NULL,NULL,NULL,NULL,NULL,NULL,'9e43bcca-66e7-4ecb-b1b6-75d2d5cc271c');
/*!40000 ALTER TABLE `concept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_answer`
--

DROP TABLE IF EXISTS `concept_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_answer` (
  `concept_answer_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `answer_concept` int(11) DEFAULT NULL,
  `answer_drug` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `sort_weight` double DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_answer_id`),
  UNIQUE KEY `concept_answer_uuid_index` (`uuid`),
  KEY `answer` (`answer_concept`),
  KEY `answers_for_concept` (`concept_id`),
  KEY `answer_creator` (`creator`),
  KEY `answer_answer_drug_fk` (`answer_drug`),
  CONSTRAINT `answer` FOREIGN KEY (`answer_concept`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `answer_answer_drug_fk` FOREIGN KEY (`answer_drug`) REFERENCES `drug` (`drug_id`),
  CONSTRAINT `answer_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `answers_for_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_answer`
--

LOCK TABLES `concept_answer` WRITE;
/*!40000 ALTER TABLE `concept_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_class`
--

DROP TABLE IF EXISTS `concept_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_class` (
  `concept_class_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_class_id`),
  UNIQUE KEY `concept_class_uuid_index` (`uuid`),
  KEY `concept_class_retired_status` (`retired`),
  KEY `concept_class_creator` (`creator`),
  KEY `user_who_retired_concept_class` (`retired_by`),
  CONSTRAINT `concept_class_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_concept_class` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_class`
--

LOCK TABLES `concept_class` WRITE;
/*!40000 ALTER TABLE `concept_class` DISABLE KEYS */;
INSERT INTO `concept_class` VALUES (1,'Test','Acq. during patient encounter (vitals, labs, etc.)',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d4907b2-c2cc-11de-8d13-0010c6dffd0f'),(2,'Procedure','Describes a clinical procedure',1,'2004-03-02 00:00:00',0,NULL,NULL,NULL,'8d490bf4-c2cc-11de-8d13-0010c6dffd0f'),(3,'Drug','Drug',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d490dfc-c2cc-11de-8d13-0010c6dffd0f'),(4,'Diagnosis','Conclusion drawn through findings',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d4918b0-c2cc-11de-8d13-0010c6dffd0f'),(5,'Finding','Practitioner observation/finding',1,'2004-03-02 00:00:00',0,NULL,NULL,NULL,'8d491a9a-c2cc-11de-8d13-0010c6dffd0f'),(6,'Anatomy','Anatomic sites / descriptors',1,'2004-03-02 00:00:00',0,NULL,NULL,NULL,'8d491c7a-c2cc-11de-8d13-0010c6dffd0f'),(7,'Question','Question (eg, patient history, SF36 items)',1,'2004-03-02 00:00:00',0,NULL,NULL,NULL,'8d491e50-c2cc-11de-8d13-0010c6dffd0f'),(8,'LabSet','Term to describe laboratory sets',1,'2004-03-02 00:00:00',0,NULL,NULL,NULL,'8d492026-c2cc-11de-8d13-0010c6dffd0f'),(9,'MedSet','Term to describe medication sets',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d4923b4-c2cc-11de-8d13-0010c6dffd0f'),(10,'ConvSet','Term to describe convenience sets',1,'2004-03-02 00:00:00',0,NULL,NULL,NULL,'8d492594-c2cc-11de-8d13-0010c6dffd0f'),(11,'Misc','Terms which don\'t fit other categories',1,'2004-03-02 00:00:00',0,NULL,NULL,NULL,'8d492774-c2cc-11de-8d13-0010c6dffd0f'),(12,'Symptom','Patient-reported observation',1,'2004-10-04 00:00:00',0,NULL,NULL,NULL,'8d492954-c2cc-11de-8d13-0010c6dffd0f'),(13,'Symptom/Finding','Observation that can be reported from patient or found on exam',1,'2004-10-04 00:00:00',0,NULL,NULL,NULL,'8d492b2a-c2cc-11de-8d13-0010c6dffd0f'),(14,'Specimen','Body or fluid specimen',1,'2004-12-02 00:00:00',0,NULL,NULL,NULL,'8d492d0a-c2cc-11de-8d13-0010c6dffd0f'),(15,'Misc Order','Orderable items which aren\'t tests or drugs',1,'2005-02-17 00:00:00',0,NULL,NULL,NULL,'8d492ee0-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `concept_class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_complex`
--

DROP TABLE IF EXISTS `concept_complex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_complex` (
  `concept_id` int(11) NOT NULL,
  `handler` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`concept_id`),
  CONSTRAINT `concept_attributes` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_complex`
--

LOCK TABLES `concept_complex` WRITE;
/*!40000 ALTER TABLE `concept_complex` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_complex` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_datatype`
--

DROP TABLE IF EXISTS `concept_datatype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_datatype` (
  `concept_datatype_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `hl7_abbreviation` varchar(3) DEFAULT NULL,
  `description` varchar(255) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_datatype_id`),
  UNIQUE KEY `concept_datatype_uuid_index` (`uuid`),
  KEY `concept_datatype_retired_status` (`retired`),
  KEY `concept_datatype_creator` (`creator`),
  KEY `user_who_retired_concept_datatype` (`retired_by`),
  CONSTRAINT `concept_datatype_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_concept_datatype` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_datatype`
--

LOCK TABLES `concept_datatype` WRITE;
/*!40000 ALTER TABLE `concept_datatype` DISABLE KEYS */;
INSERT INTO `concept_datatype` VALUES (1,'Numeric','NM','Numeric value, including integer or float (e.g., creatinine, weight)',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d4a4488-c2cc-11de-8d13-0010c6dffd0f'),(2,'Coded','CWE','Value determined by term dictionary lookup (i.e., term identifier)',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d4a48b6-c2cc-11de-8d13-0010c6dffd0f'),(3,'Text','ST','Free text',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d4a4ab4-c2cc-11de-8d13-0010c6dffd0f'),(4,'N/A','ZZ','Not associated with a datatype (e.g., term answers, sets)',1,'2004-02-02 00:00:00',0,NULL,NULL,NULL,'8d4a4c94-c2cc-11de-8d13-0010c6dffd0f'),(5,'Document','RP','Pointer to a binary or text-based document (e.g., clinical document, RTF, XML, EKG, image, etc.) stored in complex_obs table',1,'2004-04-15 00:00:00',0,NULL,NULL,NULL,'8d4a4e74-c2cc-11de-8d13-0010c6dffd0f'),(6,'Date','DT','Absolute date',1,'2004-07-22 00:00:00',0,NULL,NULL,NULL,'8d4a505e-c2cc-11de-8d13-0010c6dffd0f'),(7,'Time','TM','Absolute time of day',1,'2004-07-22 00:00:00',0,NULL,NULL,NULL,'8d4a591e-c2cc-11de-8d13-0010c6dffd0f'),(8,'Datetime','TS','Absolute date and time',1,'2004-07-22 00:00:00',0,NULL,NULL,NULL,'8d4a5af4-c2cc-11de-8d13-0010c6dffd0f'),(10,'Boolean','BIT','Boolean value (yes/no, true/false)',1,'2004-08-26 00:00:00',0,NULL,NULL,NULL,'8d4a5cca-c2cc-11de-8d13-0010c6dffd0f'),(11,'Rule','ZZ','Value derived from other data',1,'2006-09-11 00:00:00',0,NULL,NULL,NULL,'8d4a5e96-c2cc-11de-8d13-0010c6dffd0f'),(12,'Structured Numeric','SN','Complex numeric values possible (ie, <5, 1-10, etc.)',1,'2005-08-06 00:00:00',0,NULL,NULL,NULL,'8d4a606c-c2cc-11de-8d13-0010c6dffd0f'),(13,'Complex','ED','Complex value.  Analogous to HL7 Embedded Datatype',1,'2008-05-28 12:25:34',0,NULL,NULL,NULL,'8d4a6242-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `concept_datatype` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_description`
--

DROP TABLE IF EXISTS `concept_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_description` (
  `concept_description_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `locale` varchar(50) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_description_id`),
  UNIQUE KEY `concept_description_uuid_index` (`uuid`),
  KEY `user_who_changed_description` (`changed_by`),
  KEY `description_for_concept` (`concept_id`),
  KEY `user_who_created_description` (`creator`),
  CONSTRAINT `description_for_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `user_who_changed_description` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_created_description` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_description`
--

LOCK TABLES `concept_description` WRITE;
/*!40000 ALTER TABLE `concept_description` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_description` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_map_type`
--

DROP TABLE IF EXISTS `concept_map_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_map_type` (
  `concept_map_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `is_hidden` tinyint(1) NOT NULL DEFAULT '0',
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`concept_map_type_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `mapped_user_creator_concept_map_type` (`creator`),
  KEY `mapped_user_changed_concept_map_type` (`changed_by`),
  KEY `mapped_user_retired_concept_map_type` (`retired_by`),
  CONSTRAINT `mapped_user_changed_concept_map_type` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `mapped_user_creator_concept_map_type` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `mapped_user_retired_concept_map_type` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=71 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_map_type`
--

LOCK TABLES `concept_map_type` WRITE;
/*!40000 ALTER TABLE `concept_map_type` DISABLE KEYS */;
INSERT INTO `concept_map_type` VALUES (1,'SAME-AS',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'35543629-7d8c-11e1-909d-c80aa9edcf4e'),(2,'NARROWER-THAN',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'43ac5109-7d8c-11e1-909d-c80aa9edcf4e'),(3,'BROADER-THAN',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'4b9d9421-7d8c-11e1-909d-c80aa9edcf4e'),(4,'Associated finding',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'55e02065-7d8c-11e1-909d-c80aa9edcf4e'),(5,'Associated morphology',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'605f4a61-7d8c-11e1-909d-c80aa9edcf4e'),(6,'Associated procedure',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'6eb1bfce-7d8c-11e1-909d-c80aa9edcf4e'),(7,'Associated with',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'781bdc8f-7d8c-11e1-909d-c80aa9edcf4e'),(8,'Causative agent',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'808f9e19-7d8c-11e1-909d-c80aa9edcf4e'),(9,'Finding site',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'889c3013-7d8c-11e1-909d-c80aa9edcf4e'),(10,'Has specimen',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'929600b9-7d8c-11e1-909d-c80aa9edcf4e'),(11,'Laterality',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'999c6fc0-7d8c-11e1-909d-c80aa9edcf4e'),(12,'Severity',NULL,1,'2016-07-07 00:00:00',NULL,NULL,0,0,NULL,NULL,NULL,'a0e52281-7d8c-11e1-909d-c80aa9edcf4e'),(13,'Access',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'f9e90b29-7d8c-11e1-909d-c80aa9edcf4e'),(14,'After',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'01b60e29-7d8d-11e1-909d-c80aa9edcf4e'),(15,'Clinical course',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'5f7c3702-7d8d-11e1-909d-c80aa9edcf4e'),(16,'Component',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'67debecc-7d8d-11e1-909d-c80aa9edcf4e'),(17,'Direct device',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'718c00da-7d8d-11e1-909d-c80aa9edcf4e'),(18,'Direct morphology',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'7b9509cb-7d8d-11e1-909d-c80aa9edcf4e'),(19,'Direct substance',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'82bb495d-7d8d-11e1-909d-c80aa9edcf4e'),(20,'Due to',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'8b77f7d3-7d8d-11e1-909d-c80aa9edcf4e'),(21,'Episodicity',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'94a81179-7d8d-11e1-909d-c80aa9edcf4e'),(22,'Finding context',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'9d23c22e-7d8d-11e1-909d-c80aa9edcf4e'),(23,'Finding informer',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'a4524368-7d8d-11e1-909d-c80aa9edcf4e'),(24,'Finding method',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'af089254-7d8d-11e1-909d-c80aa9edcf4e'),(25,'Has active ingredient',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'b65aa605-7d8d-11e1-909d-c80aa9edcf4e'),(26,'Has definitional manifestation',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'c2b7b2fa-7d8d-11e1-909d-c80aa9edcf4'),(27,'Has dose form',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'cc3878e6-7d8d-11e1-909d-c80aa9edcf4e'),(28,'Has focus',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'d67c5840-7d8d-11e1-909d-c80aa9edcf4e'),(29,'Has intent',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'de2fb2c5-7d8d-11e1-909d-c80aa9edcf4e'),(30,'Has interpretation',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'e758838b-7d8d-11e1-909d-c80aa9edcf4e'),(31,'Indirect device',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'ee63c142-7d8d-11e1-909d-c80aa9edcf4e'),(32,'Indirect morphology',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'f4f36681-7d8d-11e1-909d-c80aa9edcf4e'),(33,'Interprets',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'fc7f5fed-7d8d-11e1-909d-c80aa9edcf4e'),(34,'Measurement method',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'06b11d79-7d8e-11e1-909d-c80aa9edcf4e'),(35,'Method',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'0efb4753-7d8e-11e1-909d-c80aa9edcf4e'),(36,'Occurrence',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'16e7b617-7d8e-11e1-909d-c80aa9edcf4e'),(37,'Part of',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'1e82007b-7d8e-11e1-909d-c80aa9edcf4e'),(38,'Pathological process',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'2969915e-7d8e-11e1-909d-c80aa9edcf4e'),(39,'Priority',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'32d57796-7d8e-11e1-909d-c80aa9edcf4e'),(40,'Procedure context',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'3f11904c-7d8e-11e1-909d-c80aa9edcf4e'),(41,'Procedure device',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'468c4aa3-7d8e-11e1-909d-c80aa9edcf4e'),(42,'Procedure morphology',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'5383e889-7d8e-11e1-909d-c80aa9edcf4e'),(43,'Procedure site',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'5ad2655d-7d8e-11e1-909d-c80aa9edcf4e'),(44,'Procedure site - Direct',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'66085196-7d8e-11e1-909d-c80aa9edcf4e'),(45,'Procedure site - Indirect',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'7080e843-7d8e-11e1-909d-c80aa9edcf4e'),(46,'Property',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'76bfb796-7d8e-11e1-909d-c80aa9edcf4e'),(47,'Recipient category',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'7e7d00e4-7d8e-11e1-909d-c80aa9edcf4e'),(48,'Revision status',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'851e14c1-7d8e-11e1-909d-c80aa9edcf4e'),(49,'Route of administration',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'8ee5b13d-7d8e-11e1-909d-c80aa9edcf4e'),(50,'Scale type',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'986acf48-7d8e-11e1-909d-c80aa9edcf4e'),(51,'Specimen procedure',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'a6937642-7d8e-11e1-909d-c80aa9edcf4e'),(52,'Specimen source identity',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'b1d6941e-7d8e-11e1-909d-c80aa9edcf4e'),(53,'Specimen source morphology',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'b7c793c1-7d8e-11e1-909d-c80aa9edcf4e'),(54,'Specimen source topography',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'be9f9eb8-7d8e-11e1-909d-c80aa9edcf4e'),(55,'Specimen substance',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'c8f2bacb-7d8e-11e1-909d-c80aa9edcf4e'),(56,'Subject of information',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'d0664c4f-7d8e-11e1-909d-c80aa9edcf4e'),(57,'Subject relationship context',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'dace9d13-7d8e-11e1-909d-c80aa9edcf4e'),(58,'Surgical approach',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'e3cd666d-7d8e-11e1-909d-c80aa9edcf4e'),(59,'Temporal context',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'ed96447d-7d8e-11e1-909d-c80aa9edcf4e'),(60,'Time aspect',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'f415bcce-7d8e-11e1-909d-c80aa9edcf4e'),(61,'Using access device',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'fa9538a9-7d8e-11e1-909d-c80aa9edcf4e'),(62,'Using device',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'06588655-7d8f-11e1-909d-c80aa9edcf4e'),(63,'Using energy',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'0c2ae0bc-7d8f-11e1-909d-c80aa9edcf4e'),(64,'Using substance',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'13d2c607-7d8f-11e1-909d-c80aa9edcf4e'),(65,'IS A',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'1ce7a784-7d8f-11e1-909d-c80aa9edcf4e'),(66,'MAY BE A',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'267812a3-7d8f-11e1-909d-c80aa9edcf4e'),(67,'MOVED FROM',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'2de3168e-7d8f-11e1-909d-c80aa9edcf4e'),(68,'MOVED TO',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'32f0fd99-7d8f-11e1-909d-c80aa9edcf4e'),(69,'REPLACED BY',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'3b3b9a7d-7d8f-11e1-909d-c80aa9edcf4e'),(70,'WAS A',NULL,1,'2016-07-07 00:00:00',NULL,NULL,1,0,NULL,NULL,NULL,'41a034da-7d8f-11e1-909d-c80aa9edcf4e');
/*!40000 ALTER TABLE `concept_map_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_name`
--

DROP TABLE IF EXISTS `concept_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_name` (
  `concept_name_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) DEFAULT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `locale` varchar(50) NOT NULL DEFAULT '',
  `locale_preferred` tinyint(1) DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `concept_name_type` varchar(50) DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_name_id`),
  UNIQUE KEY `concept_name_uuid_index` (`uuid`),
  KEY `name_of_concept` (`name`),
  KEY `name_for_concept` (`concept_id`),
  KEY `user_who_created_name` (`creator`),
  KEY `user_who_voided_this_name` (`voided_by`),
  CONSTRAINT `name_for_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `user_who_created_name` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_this_name` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_name`
--

LOCK TABLES `concept_name` WRITE;
/*!40000 ALTER TABLE `concept_name` DISABLE KEYS */;
INSERT INTO `concept_name` VALUES (1,1,'Vero','it',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'e0ff857f-0813-4352-b87d-5399ed62642a'),(2,1,'Sì','it',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'0cb29656-4572-4d7f-98b3-7aa095944a17'),(3,1,'Verdadeiro','pt',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'5c623ecc-c63c-4753-8e5e-178933b79351'),(4,1,'Sim','pt',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'998a49c7-e74f-43b5-9cdb-f8ba84705cf7'),(5,1,'Vrai','fr',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'52b6f620-d68c-4fdd-bb5f-e6d1af800d35'),(6,1,'Oui','fr',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'87d9f2a5-8393-4a2d-a8a7-eb9409e151fa'),(7,1,'True','en',1,1,'2016-07-07 08:14:50','FULLY_SPECIFIED',0,NULL,NULL,NULL,'b52447e4-5980-46f6-bc1a-3862fe6e3840'),(8,1,'Yes','en',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'e7973d60-7698-49c2-a8d7-cda2a9621926'),(9,1,'Verdadero','es',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'2c39345f-a099-4816-8ac2-7ad1424898c3'),(10,1,'Sí','es',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'f3d6b4b1-b5be-4c98-8e5a-1bc15585ed7c'),(11,2,'Falso','it',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'9aacada7-48db-4938-baef-b282fb162f76'),(12,2,'No','it',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'331de197-4b1f-4a07-af88-90b00b54da14'),(13,2,'Falso','pt',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'7937a109-24b5-4908-b78b-e6f39d490194'),(14,2,'Não','pt',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'69f96b20-ad2b-4ec1-abfd-1c911dd57478'),(15,2,'Faux','fr',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'6367664c-9307-468d-a1a3-31fd1e14c2e4'),(16,2,'Non','fr',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'c59177e6-3ba2-4888-a277-db3bd01d3bcd'),(17,2,'False','en',1,1,'2016-07-07 08:14:50','FULLY_SPECIFIED',0,NULL,NULL,NULL,'b53bb5a7-6b36-43bf-bc75-21d72c8c73d2'),(18,2,'No','en',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'81528cfc-cbae-4216-a977-4d28415a86b2'),(19,2,'Falso','es',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'dcb944aa-7eb4-498c-a770-42417a533a6a'),(20,2,'No','es',0,1,'2016-07-07 08:14:50',NULL,0,NULL,NULL,NULL,'bad743b8-2573-4100-bd27-18cbc28aac1e');
/*!40000 ALTER TABLE `concept_name` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_name_tag`
--

DROP TABLE IF EXISTS `concept_name_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_name_tag` (
  `concept_name_tag_id` int(11) NOT NULL AUTO_INCREMENT,
  `tag` varchar(50) NOT NULL,
  `description` text NOT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_name_tag_id`),
  UNIQUE KEY `concept_name_tag_unique_tags` (`tag`),
  UNIQUE KEY `concept_name_tag_uuid_index` (`uuid`),
  KEY `user_who_created_name_tag` (`creator`),
  KEY `user_who_voided_name_tag` (`voided_by`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_name_tag`
--

LOCK TABLES `concept_name_tag` WRITE;
/*!40000 ALTER TABLE `concept_name_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_name_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_name_tag_map`
--

DROP TABLE IF EXISTS `concept_name_tag_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_name_tag_map` (
  `concept_name_id` int(11) NOT NULL,
  `concept_name_tag_id` int(11) NOT NULL,
  KEY `mapped_concept_name` (`concept_name_id`),
  KEY `mapped_concept_name_tag` (`concept_name_tag_id`),
  CONSTRAINT `mapped_concept_name` FOREIGN KEY (`concept_name_id`) REFERENCES `concept_name` (`concept_name_id`),
  CONSTRAINT `mapped_concept_name_tag` FOREIGN KEY (`concept_name_tag_id`) REFERENCES `concept_name_tag` (`concept_name_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_name_tag_map`
--

LOCK TABLES `concept_name_tag_map` WRITE;
/*!40000 ALTER TABLE `concept_name_tag_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_name_tag_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_numeric`
--

DROP TABLE IF EXISTS `concept_numeric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_numeric` (
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `hi_absolute` double DEFAULT NULL,
  `hi_critical` double DEFAULT NULL,
  `hi_normal` double DEFAULT NULL,
  `low_absolute` double DEFAULT NULL,
  `low_critical` double DEFAULT NULL,
  `low_normal` double DEFAULT NULL,
  `units` varchar(50) DEFAULT NULL,
  `precise` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`concept_id`),
  CONSTRAINT `numeric_attributes` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_numeric`
--

LOCK TABLES `concept_numeric` WRITE;
/*!40000 ALTER TABLE `concept_numeric` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_numeric` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_proposal`
--

DROP TABLE IF EXISTS `concept_proposal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_proposal` (
  `concept_proposal_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) DEFAULT NULL,
  `encounter_id` int(11) DEFAULT NULL,
  `original_text` varchar(255) NOT NULL DEFAULT '',
  `final_text` varchar(255) DEFAULT NULL,
  `obs_id` int(11) DEFAULT NULL,
  `obs_concept_id` int(11) DEFAULT NULL,
  `state` varchar(32) NOT NULL DEFAULT 'UNMAPPED',
  `comments` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `locale` varchar(50) NOT NULL DEFAULT '',
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_proposal_id`),
  UNIQUE KEY `concept_proposal_uuid_index` (`uuid`),
  KEY `user_who_changed_proposal` (`changed_by`),
  KEY `concept_for_proposal` (`concept_id`),
  KEY `user_who_created_proposal` (`creator`),
  KEY `encounter_for_proposal` (`encounter_id`),
  KEY `proposal_obs_concept_id` (`obs_concept_id`),
  KEY `proposal_obs_id` (`obs_id`),
  CONSTRAINT `concept_for_proposal` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `encounter_for_proposal` FOREIGN KEY (`encounter_id`) REFERENCES `encounter` (`encounter_id`),
  CONSTRAINT `proposal_obs_concept_id` FOREIGN KEY (`obs_concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `proposal_obs_id` FOREIGN KEY (`obs_id`) REFERENCES `obs` (`obs_id`),
  CONSTRAINT `user_who_changed_proposal` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_created_proposal` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_proposal`
--

LOCK TABLES `concept_proposal` WRITE;
/*!40000 ALTER TABLE `concept_proposal` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_proposal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_proposal_tag_map`
--

DROP TABLE IF EXISTS `concept_proposal_tag_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_proposal_tag_map` (
  `concept_proposal_id` int(11) NOT NULL,
  `concept_name_tag_id` int(11) NOT NULL,
  KEY `mapped_concept_proposal_tag` (`concept_name_tag_id`),
  KEY `mapped_concept_proposal` (`concept_proposal_id`),
  CONSTRAINT `mapped_concept_proposal` FOREIGN KEY (`concept_proposal_id`) REFERENCES `concept_proposal` (`concept_proposal_id`),
  CONSTRAINT `mapped_concept_proposal_tag` FOREIGN KEY (`concept_name_tag_id`) REFERENCES `concept_name_tag` (`concept_name_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_proposal_tag_map`
--

LOCK TABLES `concept_proposal_tag_map` WRITE;
/*!40000 ALTER TABLE `concept_proposal_tag_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_proposal_tag_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_reference_map`
--

DROP TABLE IF EXISTS `concept_reference_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_reference_map` (
  `concept_map_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_reference_term_id` int(11) NOT NULL,
  `concept_map_type_id` int(11) NOT NULL DEFAULT '1',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_map_id`),
  KEY `map_for_concept` (`concept_id`),
  KEY `map_creator` (`creator`),
  KEY `mapped_concept_map_type` (`concept_map_type_id`),
  KEY `mapped_user_changed_ref_term` (`changed_by`),
  KEY `mapped_concept_reference_term` (`concept_reference_term_id`),
  CONSTRAINT `map_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `map_for_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `mapped_concept_map_type` FOREIGN KEY (`concept_map_type_id`) REFERENCES `concept_map_type` (`concept_map_type_id`),
  CONSTRAINT `mapped_concept_reference_term` FOREIGN KEY (`concept_reference_term_id`) REFERENCES `concept_reference_term` (`concept_reference_term_id`),
  CONSTRAINT `mapped_user_changed_ref_term` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_reference_map`
--

LOCK TABLES `concept_reference_map` WRITE;
/*!40000 ALTER TABLE `concept_reference_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_reference_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_reference_source`
--

DROP TABLE IF EXISTS `concept_reference_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_reference_source` (
  `concept_source_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL DEFAULT '',
  `description` text NOT NULL,
  `hl7_code` varchar(50) DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_source_id`),
  UNIQUE KEY `concept_source_unique_hl7_codes` (`hl7_code`),
  KEY `unique_hl7_code` (`hl7_code`),
  KEY `concept_source_creator` (`creator`),
  KEY `user_who_retired_concept_source` (`retired_by`),
  CONSTRAINT `concept_source_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_concept_source` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_reference_source`
--

LOCK TABLES `concept_reference_source` WRITE;
/*!40000 ALTER TABLE `concept_reference_source` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_reference_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_reference_term`
--

DROP TABLE IF EXISTS `concept_reference_term`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_reference_term` (
  `concept_reference_term_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_source_id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `code` varchar(255) NOT NULL,
  `version` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `date_changed` datetime DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`concept_reference_term_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `mapped_user_creator` (`creator`),
  KEY `mapped_user_changed` (`changed_by`),
  KEY `mapped_user_retired` (`retired_by`),
  KEY `mapped_concept_source` (`concept_source_id`),
  KEY `idx_code_concept_reference_term` (`code`),
  CONSTRAINT `mapped_concept_source` FOREIGN KEY (`concept_source_id`) REFERENCES `concept_reference_source` (`concept_source_id`),
  CONSTRAINT `mapped_user_changed` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `mapped_user_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `mapped_user_retired` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_reference_term`
--

LOCK TABLES `concept_reference_term` WRITE;
/*!40000 ALTER TABLE `concept_reference_term` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_reference_term` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_reference_term_map`
--

DROP TABLE IF EXISTS `concept_reference_term_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_reference_term_map` (
  `concept_reference_term_map_id` int(11) NOT NULL AUTO_INCREMENT,
  `term_a_id` int(11) NOT NULL,
  `term_b_id` int(11) NOT NULL,
  `a_is_to_b_id` int(11) NOT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`concept_reference_term_map_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `mapped_term_a` (`term_a_id`),
  KEY `mapped_term_b` (`term_b_id`),
  KEY `mapped_concept_map_type_ref_term_map` (`a_is_to_b_id`),
  KEY `mapped_user_creator_ref_term_map` (`creator`),
  KEY `mapped_user_changed_ref_term_map` (`changed_by`),
  CONSTRAINT `mapped_concept_map_type_ref_term_map` FOREIGN KEY (`a_is_to_b_id`) REFERENCES `concept_map_type` (`concept_map_type_id`),
  CONSTRAINT `mapped_term_a` FOREIGN KEY (`term_a_id`) REFERENCES `concept_reference_term` (`concept_reference_term_id`),
  CONSTRAINT `mapped_term_b` FOREIGN KEY (`term_b_id`) REFERENCES `concept_reference_term` (`concept_reference_term_id`),
  CONSTRAINT `mapped_user_changed_ref_term_map` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `mapped_user_creator_ref_term_map` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_reference_term_map`
--

LOCK TABLES `concept_reference_term_map` WRITE;
/*!40000 ALTER TABLE `concept_reference_term_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_reference_term_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_set`
--

DROP TABLE IF EXISTS `concept_set`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_set` (
  `concept_set_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `concept_set` int(11) NOT NULL DEFAULT '0',
  `sort_weight` double DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_set_id`),
  UNIQUE KEY `concept_set_uuid_index` (`uuid`),
  KEY `idx_concept_set_concept` (`concept_id`),
  KEY `has_a` (`concept_set`),
  KEY `user_who_created` (`creator`),
  CONSTRAINT `has_a` FOREIGN KEY (`concept_set`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `user_who_created` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_set`
--

LOCK TABLES `concept_set` WRITE;
/*!40000 ALTER TABLE `concept_set` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_set` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_set_derived`
--

DROP TABLE IF EXISTS `concept_set_derived`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_set_derived` (
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `concept_set` int(11) NOT NULL DEFAULT '0',
  `sort_weight` double DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_id`,`concept_set`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_set_derived`
--

LOCK TABLES `concept_set_derived` WRITE;
/*!40000 ALTER TABLE `concept_set_derived` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_set_derived` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_state_conversion`
--

DROP TABLE IF EXISTS `concept_state_conversion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_state_conversion` (
  `concept_state_conversion_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) DEFAULT '0',
  `program_workflow_id` int(11) DEFAULT '0',
  `program_workflow_state_id` int(11) DEFAULT '0',
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`concept_state_conversion_id`),
  UNIQUE KEY `unique_workflow_concept_in_conversion` (`program_workflow_id`,`concept_id`),
  UNIQUE KEY `concept_state_conversion_uuid_index` (`uuid`),
  KEY `concept_triggers_conversion` (`concept_id`),
  KEY `conversion_to_state` (`program_workflow_state_id`),
  CONSTRAINT `concept_triggers_conversion` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `conversion_involves_workflow` FOREIGN KEY (`program_workflow_id`) REFERENCES `program_workflow` (`program_workflow_id`),
  CONSTRAINT `conversion_to_state` FOREIGN KEY (`program_workflow_state_id`) REFERENCES `program_workflow_state` (`program_workflow_state_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_state_conversion`
--

LOCK TABLES `concept_state_conversion` WRITE;
/*!40000 ALTER TABLE `concept_state_conversion` DISABLE KEYS */;
/*!40000 ALTER TABLE `concept_state_conversion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_stop_word`
--

DROP TABLE IF EXISTS `concept_stop_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_stop_word` (
  `concept_stop_word_id` int(11) NOT NULL AUTO_INCREMENT,
  `word` varchar(50) NOT NULL,
  `locale` varchar(20) NOT NULL DEFAULT 'en',
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`concept_stop_word_id`),
  UNIQUE KEY `Unique_StopWord_Key` (`word`,`locale`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_stop_word`
--

LOCK TABLES `concept_stop_word` WRITE;
/*!40000 ALTER TABLE `concept_stop_word` DISABLE KEYS */;
INSERT INTO `concept_stop_word` VALUES (1,'A','en','f5f45540-e2a7-11df-87ae-18a905e044dc'),(2,'AND','en','f5f469ae-e2a7-11df-87ae-18a905e044dc'),(3,'AT','en','f5f47070-e2a7-11df-87ae-18a905e044dc'),(4,'BUT','en','f5f476c4-e2a7-11df-87ae-18a905e044dc'),(5,'BY','en','f5f47d04-e2a7-11df-87ae-18a905e044dc'),(6,'FOR','en','f5f4834e-e2a7-11df-87ae-18a905e044dc'),(7,'HAS','en','f5f48a24-e2a7-11df-87ae-18a905e044dc'),(8,'OF','en','f5f49064-e2a7-11df-87ae-18a905e044dc'),(9,'THE','en','f5f496ae-e2a7-11df-87ae-18a905e044dc'),(10,'TO','en','f5f49cda-e2a7-11df-87ae-18a905e044dc');
/*!40000 ALTER TABLE `concept_stop_word` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `concept_word`
--

DROP TABLE IF EXISTS `concept_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `concept_word` (
  `concept_word_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `word` varchar(50) NOT NULL DEFAULT '',
  `locale` varchar(20) NOT NULL DEFAULT '',
  `concept_name_id` int(11) NOT NULL,
  `weight` double DEFAULT '1',
  PRIMARY KEY (`concept_word_id`),
  KEY `word_in_concept_name` (`word`),
  KEY `concept_word_concept_idx` (`concept_id`),
  KEY `concept_word_weight_index` (`weight`),
  KEY `word_for_name` (`concept_name_id`),
  CONSTRAINT `word_for` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `word_for_name` FOREIGN KEY (`concept_name_id`) REFERENCES `concept_name` (`concept_name_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `concept_word`
--

LOCK TABLES `concept_word` WRITE;
/*!40000 ALTER TABLE `concept_word` DISABLE KEYS */;
INSERT INTO `concept_word` VALUES (21,1,'VERDADERO','es',9,8.45679012345679),(22,1,'SÌ','it',2,15.15),(23,1,'VRAI','fr',5,10.5),(24,1,'YES','en',8,11.911111111111111),(25,1,'SÍ','es',10,15.15),(26,1,'VERDADEIRO','pt',3,8.31),(27,1,'SIM','pt',4,11.911111111111111),(28,1,'OUI','fr',6,11.911111111111111),(29,1,'VERO','it',1,10.5),(30,1,'TRUE','en',7,10.6875),(31,2,'NO','es',20,15.15),(32,2,'FAUX','fr',15,10.5),(33,2,'FALSO','pt',13,9.72),(34,2,'NO','en',18,15.15),(35,2,'NÃO','pt',14,11.911111111111111),(36,2,'FALSO','it',11,9.72),(37,2,'NON','fr',16,11.911111111111111),(38,2,'FALSE','en',17,9.9),(39,2,'NO','it',12,15.15),(40,2,'FALSO','es',19,9.72);
/*!40000 ALTER TABLE `concept_word` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drug`
--

DROP TABLE IF EXISTS `drug`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `drug` (
  `drug_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `name` varchar(255) DEFAULT NULL,
  `combination` tinyint(1) NOT NULL DEFAULT '0',
  `dosage_form` int(11) DEFAULT NULL,
  `dose_strength` double DEFAULT NULL,
  `maximum_daily_dose` double DEFAULT NULL,
  `minimum_daily_dose` double DEFAULT NULL,
  `route` int(11) DEFAULT NULL,
  `units` varchar(50) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`drug_id`),
  UNIQUE KEY `drug_uuid_index` (`uuid`),
  KEY `primary_drug_concept` (`concept_id`),
  KEY `drug_creator` (`creator`),
  KEY `drug_changed_by` (`changed_by`),
  KEY `dosage_form_concept` (`dosage_form`),
  KEY `drug_retired_by` (`retired_by`),
  KEY `route_concept` (`route`),
  CONSTRAINT `dosage_form_concept` FOREIGN KEY (`dosage_form`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `drug_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `drug_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `drug_retired_by` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `primary_drug_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `route_concept` FOREIGN KEY (`route`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drug`
--

LOCK TABLES `drug` WRITE;
/*!40000 ALTER TABLE `drug` DISABLE KEYS */;
/*!40000 ALTER TABLE `drug` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drug_ingredient`
--

DROP TABLE IF EXISTS `drug_ingredient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `drug_ingredient` (
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `ingredient_id` int(11) NOT NULL DEFAULT '0',
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`ingredient_id`,`concept_id`),
  KEY `combination_drug` (`concept_id`),
  CONSTRAINT `combination_drug` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drug_ingredient`
--

LOCK TABLES `drug_ingredient` WRITE;
/*!40000 ALTER TABLE `drug_ingredient` DISABLE KEYS */;
/*!40000 ALTER TABLE `drug_ingredient` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drug_order`
--

DROP TABLE IF EXISTS `drug_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `drug_order` (
  `order_id` int(11) NOT NULL DEFAULT '0',
  `drug_inventory_id` int(11) DEFAULT '0',
  `dose` double DEFAULT NULL,
  `equivalent_daily_dose` double DEFAULT NULL,
  `units` varchar(255) DEFAULT NULL,
  `frequency` varchar(255) DEFAULT NULL,
  `prn` tinyint(1) NOT NULL DEFAULT '0',
  `complex` tinyint(1) NOT NULL DEFAULT '0',
  `quantity` int(11) DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `inventory_item` (`drug_inventory_id`),
  CONSTRAINT `extends_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `inventory_item` FOREIGN KEY (`drug_inventory_id`) REFERENCES `drug` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drug_order`
--

LOCK TABLES `drug_order` WRITE;
/*!40000 ALTER TABLE `drug_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `drug_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `encounter`
--

DROP TABLE IF EXISTS `encounter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `encounter` (
  `encounter_id` int(11) NOT NULL AUTO_INCREMENT,
  `encounter_type` int(11) NOT NULL,
  `patient_id` int(11) NOT NULL DEFAULT '0',
  `location_id` int(11) DEFAULT NULL,
  `form_id` int(11) DEFAULT NULL,
  `encounter_datetime` datetime NOT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `visit_id` int(11) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`encounter_id`),
  UNIQUE KEY `encounter_uuid_index` (`uuid`),
  KEY `encounter_datetime_idx` (`encounter_datetime`),
  KEY `encounter_ibfk_1` (`creator`),
  KEY `encounter_type_id` (`encounter_type`),
  KEY `encounter_form` (`form_id`),
  KEY `encounter_location` (`location_id`),
  KEY `encounter_patient` (`patient_id`),
  KEY `user_who_voided_encounter` (`voided_by`),
  KEY `encounter_changed_by` (`changed_by`),
  KEY `encounter_visit_id_fk` (`visit_id`),
  CONSTRAINT `encounter_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `encounter_form` FOREIGN KEY (`form_id`) REFERENCES `form` (`form_id`),
  CONSTRAINT `encounter_ibfk_1` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `encounter_location` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
  CONSTRAINT `encounter_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON UPDATE CASCADE,
  CONSTRAINT `encounter_type_id` FOREIGN KEY (`encounter_type`) REFERENCES `encounter_type` (`encounter_type_id`),
  CONSTRAINT `encounter_visit_id_fk` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`visit_id`),
  CONSTRAINT `user_who_voided_encounter` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `encounter`
--

LOCK TABLES `encounter` WRITE;
/*!40000 ALTER TABLE `encounter` DISABLE KEYS */;
/*!40000 ALTER TABLE `encounter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `encounter_provider`
--

DROP TABLE IF EXISTS `encounter_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `encounter_provider` (
  `encounter_provider_id` int(11) NOT NULL AUTO_INCREMENT,
  `encounter_id` int(11) NOT NULL,
  `provider_id` int(11) NOT NULL,
  `encounter_role_id` int(11) NOT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `date_voided` datetime DEFAULT NULL,
  `voided_by` int(11) DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`encounter_provider_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `encounter_id_fk` (`encounter_id`),
  KEY `provider_id_fk` (`provider_id`),
  KEY `encounter_role_id_fk` (`encounter_role_id`),
  KEY `encounter_provider_creator` (`creator`),
  KEY `encounter_provider_changed_by` (`changed_by`),
  KEY `encounter_provider_voided_by` (`voided_by`),
  CONSTRAINT `encounter_id_fk` FOREIGN KEY (`encounter_id`) REFERENCES `encounter` (`encounter_id`),
  CONSTRAINT `encounter_provider_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `encounter_provider_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `encounter_provider_voided_by` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `encounter_role_id_fk` FOREIGN KEY (`encounter_role_id`) REFERENCES `encounter_role` (`encounter_role_id`),
  CONSTRAINT `provider_id_fk` FOREIGN KEY (`provider_id`) REFERENCES `provider` (`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `encounter_provider`
--

LOCK TABLES `encounter_provider` WRITE;
/*!40000 ALTER TABLE `encounter_provider` DISABLE KEYS */;
/*!40000 ALTER TABLE `encounter_provider` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `encounter_role`
--

DROP TABLE IF EXISTS `encounter_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `encounter_role` (
  `encounter_role_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`encounter_role_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `encounter_role_creator_fk` (`creator`),
  KEY `encounter_role_changed_by_fk` (`changed_by`),
  KEY `encounter_role_retired_by_fk` (`retired_by`),
  CONSTRAINT `encounter_role_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `encounter_role_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `encounter_role_retired_by_fk` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `encounter_role`
--

LOCK TABLES `encounter_role` WRITE;
/*!40000 ALTER TABLE `encounter_role` DISABLE KEYS */;
INSERT INTO `encounter_role` VALUES (1,'Unknown','Unknown encounter role for legacy providers with no encounter role set',1,'2011-08-18 14:00:00',NULL,NULL,0,NULL,NULL,NULL,'a0b03050-c99b-11e0-9572-0800200c9a66');
/*!40000 ALTER TABLE `encounter_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `encounter_type`
--

DROP TABLE IF EXISTS `encounter_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `encounter_type` (
  `encounter_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL DEFAULT '',
  `description` text,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`encounter_type_id`),
  UNIQUE KEY `encounter_type_uuid_index` (`uuid`),
  KEY `encounter_type_retired_status` (`retired`),
  KEY `user_who_created_type` (`creator`),
  KEY `user_who_retired_encounter_type` (`retired_by`),
  CONSTRAINT `user_who_created_type` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_encounter_type` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `encounter_type`
--

LOCK TABLES `encounter_type` WRITE;
/*!40000 ALTER TABLE `encounter_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `encounter_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `field`
--

DROP TABLE IF EXISTS `field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `field` (
  `field_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` text,
  `field_type` int(11) DEFAULT NULL,
  `concept_id` int(11) DEFAULT NULL,
  `table_name` varchar(50) DEFAULT NULL,
  `attribute_name` varchar(50) DEFAULT NULL,
  `default_value` text,
  `select_multiple` tinyint(1) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`field_id`),
  UNIQUE KEY `field_uuid_index` (`uuid`),
  KEY `field_retired_status` (`retired`),
  KEY `user_who_changed_field` (`changed_by`),
  KEY `concept_for_field` (`concept_id`),
  KEY `user_who_created_field` (`creator`),
  KEY `type_of_field` (`field_type`),
  KEY `user_who_retired_field` (`retired_by`),
  CONSTRAINT `concept_for_field` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `type_of_field` FOREIGN KEY (`field_type`) REFERENCES `field_type` (`field_type_id`),
  CONSTRAINT `user_who_changed_field` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_created_field` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_field` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `field`
--

LOCK TABLES `field` WRITE;
/*!40000 ALTER TABLE `field` DISABLE KEYS */;
/*!40000 ALTER TABLE `field` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `field_answer`
--

DROP TABLE IF EXISTS `field_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `field_answer` (
  `field_id` int(11) NOT NULL DEFAULT '0',
  `answer_id` int(11) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`field_id`,`answer_id`),
  UNIQUE KEY `field_answer_uuid_index` (`uuid`),
  KEY `field_answer_concept` (`answer_id`),
  KEY `user_who_created_field_answer` (`creator`),
  CONSTRAINT `answers_for_field` FOREIGN KEY (`field_id`) REFERENCES `field` (`field_id`),
  CONSTRAINT `field_answer_concept` FOREIGN KEY (`answer_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `user_who_created_field_answer` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `field_answer`
--

LOCK TABLES `field_answer` WRITE;
/*!40000 ALTER TABLE `field_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `field_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `field_type`
--

DROP TABLE IF EXISTS `field_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `field_type` (
  `field_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `description` text,
  `is_set` tinyint(1) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`field_type_id`),
  UNIQUE KEY `field_type_uuid_index` (`uuid`),
  KEY `user_who_created_field_type` (`creator`),
  CONSTRAINT `user_who_created_field_type` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `field_type`
--

LOCK TABLES `field_type` WRITE;
/*!40000 ALTER TABLE `field_type` DISABLE KEYS */;
INSERT INTO `field_type` VALUES (1,'Concept','',0,1,'2005-02-22 00:00:00','8d5e7d7c-c2cc-11de-8d13-0010c6dffd0f'),(2,'Database element','',0,1,'2005-02-22 00:00:00','8d5e8196-c2cc-11de-8d13-0010c6dffd0f'),(3,'Set of Concepts','',1,1,'2005-02-22 00:00:00','8d5e836c-c2cc-11de-8d13-0010c6dffd0f'),(4,'Miscellaneous Set','',1,1,'2005-02-22 00:00:00','8d5e852e-c2cc-11de-8d13-0010c6dffd0f'),(5,'Section','',1,1,'2005-02-22 00:00:00','8d5e86fa-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `field_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `form`
--

DROP TABLE IF EXISTS `form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `form` (
  `form_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `version` varchar(50) NOT NULL DEFAULT '',
  `build` int(11) DEFAULT NULL,
  `published` tinyint(1) NOT NULL DEFAULT '0',
  `xslt` text,
  `template` text,
  `description` text,
  `encounter_type` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retired_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`form_id`),
  UNIQUE KEY `form_uuid_index` (`uuid`),
  KEY `form_published_index` (`published`),
  KEY `form_retired_index` (`retired`),
  KEY `form_published_and_retired_index` (`published`,`retired`),
  KEY `user_who_last_changed_form` (`changed_by`),
  KEY `user_who_created_form` (`creator`),
  KEY `form_encounter_type` (`encounter_type`),
  KEY `user_who_retired_form` (`retired_by`),
  CONSTRAINT `form_encounter_type` FOREIGN KEY (`encounter_type`) REFERENCES `encounter_type` (`encounter_type_id`),
  CONSTRAINT `user_who_created_form` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_last_changed_form` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_form` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `form`
--

LOCK TABLES `form` WRITE;
/*!40000 ALTER TABLE `form` DISABLE KEYS */;
/*!40000 ALTER TABLE `form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `form_field`
--

DROP TABLE IF EXISTS `form_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `form_field` (
  `form_field_id` int(11) NOT NULL AUTO_INCREMENT,
  `form_id` int(11) NOT NULL DEFAULT '0',
  `field_id` int(11) NOT NULL DEFAULT '0',
  `field_number` int(11) DEFAULT NULL,
  `field_part` varchar(5) DEFAULT NULL,
  `page_number` int(11) DEFAULT NULL,
  `parent_form_field` int(11) DEFAULT NULL,
  `min_occurs` int(11) DEFAULT NULL,
  `max_occurs` int(11) DEFAULT NULL,
  `required` tinyint(1) NOT NULL DEFAULT '0',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `sort_weight` double DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`form_field_id`),
  UNIQUE KEY `form_field_uuid_index` (`uuid`),
  KEY `user_who_last_changed_form_field` (`changed_by`),
  KEY `user_who_created_form_field` (`creator`),
  KEY `field_within_form` (`field_id`),
  KEY `form_containing_field` (`form_id`),
  KEY `form_field_hierarchy` (`parent_form_field`),
  CONSTRAINT `field_within_form` FOREIGN KEY (`field_id`) REFERENCES `field` (`field_id`),
  CONSTRAINT `form_containing_field` FOREIGN KEY (`form_id`) REFERENCES `form` (`form_id`),
  CONSTRAINT `form_field_hierarchy` FOREIGN KEY (`parent_form_field`) REFERENCES `form_field` (`form_field_id`),
  CONSTRAINT `user_who_created_form_field` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_last_changed_form_field` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `form_field`
--

LOCK TABLES `form_field` WRITE;
/*!40000 ALTER TABLE `form_field` DISABLE KEYS */;
/*!40000 ALTER TABLE `form_field` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `form_resource`
--

DROP TABLE IF EXISTS `form_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `form_resource` (
  `form_resource_id` int(11) NOT NULL AUTO_INCREMENT,
  `form_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `value_reference` text NOT NULL,
  `datatype` varchar(255) DEFAULT NULL,
  `datatype_config` text,
  `preferred_handler` varchar(255) DEFAULT NULL,
  `handler_config` text,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`form_resource_id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `unique_form_and_name` (`form_id`,`name`),
  CONSTRAINT `form_resource_form_fk` FOREIGN KEY (`form_id`) REFERENCES `form` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `form_resource`
--

LOCK TABLES `form_resource` WRITE;
/*!40000 ALTER TABLE `form_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `form_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `global_property`
--

DROP TABLE IF EXISTS `global_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `global_property` (
  `property` varchar(255) NOT NULL DEFAULT '',
  `property_value` text,
  `description` text,
  `uuid` char(38) DEFAULT NULL,
  `datatype` varchar(255) DEFAULT NULL,
  `datatype_config` text,
  `preferred_handler` varchar(255) DEFAULT NULL,
  `handler_config` text,
  PRIMARY KEY (`property`),
  UNIQUE KEY `global_property_uuid_index` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `global_property`
--

LOCK TABLES `global_property` WRITE;
/*!40000 ALTER TABLE `global_property` DISABLE KEYS */;
INSERT INTO `global_property` VALUES ('application.name','OpenMRS','The name of this application, as presented to the user, for example on the login and welcome pages.','2720b841-bf4e-426f-9817-dc0118185fb5',NULL,NULL,NULL,NULL),('concept.causeOfDeath','5002','Concept id of the concept defining the CAUSE OF DEATH concept','35a0b3cd-a0a1-41d9-8d41-ba9d0d68a720',NULL,NULL,NULL,NULL),('concept.cd4_count','5497','Concept id of the concept defining the CD4 count concept','5a39a74c-fadf-4d1b-81b7-70b9ec22209b',NULL,NULL,NULL,NULL),('concept.defaultConceptMapType','NARROWER-THAN','Default concept map type which is used when no other is set','cf2f8f45-9643-4ba8-9ae9-3f8a205db591',NULL,NULL,NULL,NULL),('concept.false','2','Concept id of the concept defining the FALSE boolean concept','de4df46b-d909-4b46-a4a7-d697583400e3',NULL,NULL,NULL,NULL),('concept.height','5090','Concept id of the concept defining the HEIGHT concept','436fd51c-cdec-4830-9d23-fc1b0b73b768',NULL,NULL,NULL,NULL),('concept.medicalRecordObservations','1238','The concept id of the MEDICAL_RECORD_OBSERVATIONS concept.  This concept_id is presumed to be the generic grouping (obr) concept in hl7 messages.  An obs_group row is not created for this concept.','2e72b3bc-d5fb-4b67-85d2-06d169de6c5d',NULL,NULL,NULL,NULL),('concept.none','1107','Concept id of the concept defining the NONE concept','b6e81798-dad7-4ea5-8a04-b456ad8fbe80',NULL,NULL,NULL,NULL),('concept.otherNonCoded','5622','Concept id of the concept defining the OTHER NON-CODED concept','cf9cb0aa-5c54-453c-9021-96257ef08a01',NULL,NULL,NULL,NULL),('concept.patientDied','1742','Concept id of the concept defining the PATIENT DIED concept','4328fe1f-4065-4368-ab85-606d2f73169a',NULL,NULL,NULL,NULL),('concept.problemList','1284','The concept id of the PROBLEM LIST concept.  This concept_id is presumed to be the generic grouping (obr) concept in hl7 messages.  An obs_group row is not created for this concept.','5821d12f-dc07-4d4a-b255-804917b15913',NULL,NULL,NULL,NULL),('concept.reasonExitedCare',NULL,'Concept id of the concept defining the REASON EXITED CARE concept','31ea6495-642c-4951-b1e4-d8b7853a894c',NULL,NULL,NULL,NULL),('concept.reasonOrderStopped','1812','Concept id of the concept defining the REASON ORDER STOPPED concept','fb74e000-4f43-4f84-972a-7704721b0f99',NULL,NULL,NULL,NULL),('concept.true','1','Concept id of the concept defining the TRUE boolean concept','b2b0b49c-7999-4f26-b8c6-fe0723b0dfee',NULL,NULL,NULL,NULL),('concept.weight','5089','Concept id of the concept defining the WEIGHT concept','9e8771ad-f05b-4dc0-9860-1aae4b33ffeb',NULL,NULL,NULL,NULL),('conceptDrug.dosageForm.conceptClasses',NULL,'A comma-separated list of the allowed concept classes for the dosage form field of the concept drug management form.','c80aea2f-cccc-4299-9fe5-541fcc179f8d',NULL,NULL,NULL,NULL),('conceptDrug.route.conceptClasses',NULL,'A comma-separated list of the allowed concept classes for the route field of the concept drug management form.','27f9bcc5-1042-4851-8f26-4f5239f38547',NULL,NULL,NULL,NULL),('concepts.locked','false','if true, do not allow editing concepts','39bbd058-6634-4bf7-97e1-17772f62636a','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('concept_map_type_management.enable','false','Enables or disables management of concept map types','6fbdee26-ba1e-4a7e-a587-0914fd2ebc1d','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('dashboard.encounters.maximumNumberToShow',NULL,'An integer which, if specified, would determine the maximum number of encounters to display on the encounter tab of the patient dashboard.','137d6a22-0808-4565-adf6-99ef9a90aee7',NULL,NULL,NULL,NULL),('dashboard.encounters.providerDisplayRoles',NULL,'A comma-separated list of encounter roles (by name or id). Providers with these roles in an encounter will be displayed on the encounter tab of the patient dashboard.','bae250ec-54f8-4733-a381-51d3a5428678',NULL,NULL,NULL,NULL),('dashboard.encounters.showEditLink','true','true/false whether or not to show the \'Edit Encounter\' link on the patient dashboard','eca227fc-44c0-438a-8c34-cccac2f0e336','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('dashboard.encounters.showEmptyFields','true','true/false whether or not to show empty fields on the \'View Encounter\' window','cda1b34b-dd04-4c8b-84bf-da09c9631eed','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('dashboard.encounters.showViewLink','true','true/false whether or not to show the \'View Encounter\' link on the patient dashboard','018a4f20-9b14-4d90-9ecb-23a87a1b4c30','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('dashboard.encounters.usePages','smart','true/false/smart on how to show the pages on the \'View Encounter\' window.  \'smart\' means that if > 50% of the fields have page numbers defined, show data in pages','256cf60a-3d55-4233-856d-baa6f9610269',NULL,NULL,NULL,NULL),('dashboard.header.programs_to_show',NULL,'List of programs to show Enrollment details of in the patient header. (Should be an ordered comma-separated list of program_ids or names.)','00292469-a526-45fd-9917-bc505c985cc6',NULL,NULL,NULL,NULL),('dashboard.header.workflows_to_show',NULL,'List of programs to show Enrollment details of in the patient header. List of workflows to show current status of in the patient header. These will only be displayed if they belong to a program listed above. (Should be a comma-separated list of program_workflow_ids.)','01ab64ea-d931-4c4d-9e1f-df37577bece7',NULL,NULL,NULL,NULL),('dashboard.overview.showConcepts',NULL,'Comma delimited list of concepts ids to show on the patient dashboard overview tab','c327013f-1fc7-422c-a9cf-15ffeb5f60ca',NULL,NULL,NULL,NULL),('dashboard.regimen.displayDrugSetIds','ANTIRETROVIRAL DRUGS,TUBERCULOSIS TREATMENT DRUGS','Drug sets that appear on the Patient Dashboard Regimen tab. Comma separated list of name of concepts that are defined as drug sets.','d9f957ed-fc19-497a-acb6-73f4326c141e',NULL,NULL,NULL,NULL),('dashboard.regimen.displayFrequencies','7 days/week,6 days/week,5 days/week,4 days/week,3 days/week,2 days/week,1 days/week','Frequency of a drug order that appear on the Patient Dashboard. Comma separated list of name of concepts that are defined as drug frequencies.','da0ee700-e81d-49e1-b4c3-677b7ad9b414',NULL,NULL,NULL,NULL),('dashboard.regimen.standardRegimens','<list>  <regimenSuggestion>    <drugComponents>      <drugSuggestion>        <drugId>2</drugId>        <dose>1</dose>        <units>tab(s)</units>        <frequency>2/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>    </drugComponents>    <displayName>3TC + d4T(30) + NVP (Triomune-30)</displayName>    <codeName>standardTri30</codeName>    <canReplace>ANTIRETROVIRAL DRUGS</canReplace>  </regimenSuggestion>  <regimenSuggestion>    <drugComponents>      <drugSuggestion>        <drugId>3</drugId>        <dose>1</dose>        <units>tab(s)</units>        <frequency>2/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>    </drugComponents>    <displayName>3TC + d4T(40) + NVP (Triomune-40)</displayName>    <codeName>standardTri40</codeName>    <canReplace>ANTIRETROVIRAL DRUGS</canReplace>  </regimenSuggestion>  <regimenSuggestion>    <drugComponents>      <drugSuggestion>        <drugId>39</drugId>        <dose>1</dose>        <units>tab(s)</units>        <frequency>2/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>      <drugSuggestion>        <drugId>22</drugId>        <dose>200</dose>        <units>mg</units>        <frequency>2/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>    </drugComponents>    <displayName>AZT + 3TC + NVP</displayName>    <codeName>standardAztNvp</codeName>    <canReplace>ANTIRETROVIRAL DRUGS</canReplace>  </regimenSuggestion>  <regimenSuggestion>    <drugComponents>      <drugSuggestion reference=\"../../../regimenSuggestion[3]/drugComponents/drugSuggestion\"/>      <drugSuggestion>        <drugId>11</drugId>        <dose>600</dose>        <units>mg</units>        <frequency>1/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>    </drugComponents>    <displayName>AZT + 3TC + EFV(600)</displayName>    <codeName>standardAztEfv</codeName>    <canReplace>ANTIRETROVIRAL DRUGS</canReplace>  </regimenSuggestion>  <regimenSuggestion>    <drugComponents>      <drugSuggestion>        <drugId>5</drugId>        <dose>30</dose>        <units>mg</units>        <frequency>2/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>      <drugSuggestion>        <drugId>42</drugId>        <dose>150</dose>        		<units>mg</units>        <frequency>2/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>      <drugSuggestion reference=\"../../../regimenSuggestion[4]/drugComponents/drugSuggestion[2]\"/>    </drugComponents>    <displayName>d4T(30) + 3TC + EFV(600)</displayName>    <codeName>standardD4t30Efv</codeName>    <canReplace>ANTIRETROVIRAL DRUGS</canReplace>  </regimenSuggestion>  <regimenSuggestion>    <drugComponents>      <drugSuggestion>        <drugId>6</drugId>        <dose>40</dose>        <units>mg</units>        <frequency>2/day x 7 days/week</frequency>        <instructions></instructions>      </drugSuggestion>      <drugSuggestion reference=\"../../../regimenSuggestion[5]/drugComponents/drugSuggestion[2]\"/>      <drugSuggestion reference=\"../../../regimenSuggestion[4]/drugComponents/drugSuggestion[2]\"/>    </drugComponents>    <displayName>d4T(40) + 3TC + EFV(600)</displayName>    <codeName>standardD4t40Efv</codeName>    <canReplace>ANTIRETROVIRAL DRUGS</canReplace>  </regimenSuggestion></list>','XML description of standard drug regimens, to be shown as shortcuts on the dashboard regimen entry tab','bbd022f0-0f35-4c6c-94d2-2f2308b56371',NULL,NULL,NULL,NULL),('dashboard.relationships.show_types',NULL,'Types of relationships separated by commas.  Doctor/Patient,Parent/Child','e985abd8-79b6-4f69-b96c-c9a13d3ef14a',NULL,NULL,NULL,NULL),('dashboard.showPatientName','false','Whether or not to display the patient name in the patient dashboard title. Note that enabling this could be security risk if multiple users operate on the same computer.','83aafe2e-526e-4447-bed8-043edb069a37','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('default_locale','en_GB','Specifies the default locale. You can specify both the language code(ISO-639) and the country code(ISO-3166), e.g. \'en_GB\' or just country: e.g. \'en\'','71870828-e88b-4d0b-9c41-47ec2586d064',NULL,NULL,NULL,NULL),('default_location','Unknown Location','The name of the location to use as a system default','ff16a585-5ea7-4272-838c-ef9241359592',NULL,NULL,NULL,NULL),('default_theme',NULL,'Default theme for users.  OpenMRS ships with themes of \'green\', \'orange\', \'purple\', and \'legacy\'','98a545dc-2879-445b-ac98-e7dc0b7d19a1',NULL,NULL,NULL,NULL),('encounterForm.obsSortOrder','number','The sort order for the obs listed on the encounter edit form.  \'number\' sorts on the associated numbering from the form schema.  \'weight\' sorts on the order displayed in the form schema.','090c82b2-6226-4cde-be50-17b67e097467',NULL,NULL,NULL,NULL),('EncounterType.encounterTypes.locked','false','saving, retiring or deleting an Encounter Type is not permitted, if true','7435645c-694b-4620-84b6-58d9307ac2e2','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('FormEntry.enableDashboardTab','true','true/false whether or not to show a Form Entry tab on the patient dashboard','e4039d7a-87c1-4a76-9422-721c4c6de656','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('FormEntry.enableOnEncounterTab','false','true/false whether or not to show a Enter Form button on the encounters tab of the patient dashboard','6579a662-7b46-4d29-8c03-d520d03d3ef2','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('graph.color.absolute','rgb(20,20,20)','Color of the \'invalid\' section of numeric graphs on the patient dashboard.','8bed2d3f-aaf4-4423-ac25-4d22277b323e',NULL,NULL,NULL,NULL),('graph.color.critical','rgb(200,0,0)','Color of the \'critical\' section of numeric graphs on the patient dashboard.','ee9d5142-b689-47b6-85ff-2b23d1b9428c',NULL,NULL,NULL,NULL),('graph.color.normal','rgb(255,126,0)','Color of the \'normal\' section of numeric graphs on the patient dashboard.','0ff2bc6b-8822-473c-a123-2d530df78f77',NULL,NULL,NULL,NULL),('gzip.enabled','false','Set to \'true\' to turn on OpenMRS\'s gzip filter, and have the webapp compress data before sending it to any client that supports it. Generally use this if you are running Tomcat standalone. If you are running Tomcat behind Apache, then you\'d want to use Apache to do gzip compression.','a7608c0b-d774-4844-aa9c-ca9fe44747dc','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('hl7_archive.dir','hl7_archives','The default name or absolute path for the folder where to write the hl7_in_archives.','8098ed61-38cd-4b6f-8509-a55af165f52d',NULL,NULL,NULL,NULL),('hl7_processor.ignore_missing_patient_non_local','false','If true, hl7 messages for patients that are not found and are non-local will silently be dropped/ignored','c44f6633-e89f-4593-8c50-0a0a0058087c','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('layout.address.format','<org.openmrs.layout.web.address.AddressTemplate>\n    <nameMappings class=\"properties\">\n      <property name=\"postalCode\" value=\"Location.postalCode\"/>\n      <property name=\"longitude\" value=\"Location.longitude\"/>\n      <property name=\"address2\" value=\"Location.address2\"/>\n      <property name=\"address1\" value=\"Location.address1\"/>\n      <property name=\"startDate\" value=\"PersonAddress.startDate\"/>\n      <property name=\"country\" value=\"Location.country\"/>\n      <property name=\"endDate\" value=\"personAddress.endDate\"/>\n      <property name=\"stateProvince\" value=\"Location.stateProvince\"/>\n      <property name=\"latitude\" value=\"Location.latitude\"/>\n      <property name=\"cityVillage\" value=\"Location.cityVillage\"/>\n    </nameMappings>\n    <sizeMappings class=\"properties\">\n      <property name=\"postalCode\" value=\"10\"/>\n      <property name=\"longitude\" value=\"10\"/>\n      <property name=\"address2\" value=\"40\"/>\n      <property name=\"address1\" value=\"40\"/>\n      <property name=\"startDate\" value=\"10\"/>\n      <property name=\"country\" value=\"10\"/>\n      <property name=\"endDate\" value=\"10\"/>\n      <property name=\"stateProvince\" value=\"10\"/>\n      <property name=\"latitude\" value=\"10\"/>\n      <property name=\"cityVillage\" value=\"10\"/>\n    </sizeMappings>\n    <lineByLineFormat>\n      <string>address1</string>\n      <string>address2</string>\n      <string>cityVillage stateProvince country postalCode</string>\n      <string>latitude longitude</string>\n      <string>startDate endDate</string>\n    </lineByLineFormat>\n  </org.openmrs.layout.web.address.AddressTemplate>','XML description of address formats','98fa7af1-d183-4f70-93fb-2f488000ce4c',NULL,NULL,NULL,NULL),('layout.name.format','short','Format in which to display the person names.  Valid values are short, long','8ece8adf-4d79-4b52-a233-66187cb72f3a',NULL,NULL,NULL,NULL),('locale.allowed.list','en, es, fr, it, pt','Comma delimited list of locales allowed for use on system','2b4d2797-e8d1-4483-a863-02cfb4299212',NULL,NULL,NULL,NULL),('location.field.style','default','Type of widget to use for location fields','e77923c2-d262-4f8c-b8fe-40773827ea4d',NULL,NULL,NULL,NULL),('log.layout','%p - %C{1}.%M(%L) |%d{ISO8601}| %m%n','A log layout pattern which is used by the OpenMRS file appender.','44dd2b3e-af42-4abf-ad6e-eadb48a89d41',NULL,NULL,NULL,NULL),('log.level','org.openmrs.api:info','Logging levels for log4j.xml. Valid format is class:level,class:level. If class not specified, \'org.openmrs.api\' presumed. Valid levels are trace, debug, info, warn, error or fatal','cf66565e-d00b-4127-a774-07bad9e486fc',NULL,NULL,NULL,NULL),('log.location',NULL,'A directory where the OpenMRS log file appender is stored. The log file name is \'openmrs.log\'.','4683b0d6-45aa-4fab-a257-ae754ef1e855',NULL,NULL,NULL,NULL),('logic.default.ruleClassDirectory','logic/class','Default folder where compiled rule will be stored','e3231a20-9b7c-4101-8bbb-bfad2faa6fb5',NULL,NULL,NULL,NULL),('logic.default.ruleJavaDirectory','logic/sources','Default folder where rule\'s java file will be stored','6cca96f1-7139-44dc-a7d5-891530a26987',NULL,NULL,NULL,NULL),('logic.defaultTokens.conceptClasses',NULL,'When registering default tokens for logic, if you specify a comma-separated list of concept class names here, only concepts of those classes will have tokens registered. If you leave this blank, all classes will have tokens registered for their concepts.','13ac9af0-1f9b-405a-9f0f-5e3f00a37ce5',NULL,NULL,NULL,NULL),('logic.mandatory','false','true/false whether or not the logic module MUST start when openmrs starts.  This is used to make sure that mission critical modules are always running if openmrs is running.','b3d2befc-61c7-4d39-b8ee-436ccb05fc16',NULL,NULL,NULL,NULL),('logic.started','true','DO NOT MODIFY. true/false whether or not the logic module has been started.  This is used to make sure modules that were running  prior to a restart are started again','21a52a97-c08d-4ea0-9404-efec9b204bbc',NULL,NULL,NULL,NULL),('mail.debug','false','true/false whether to print debugging information during mailing','76c4f916-9fe1-45b5-9773-224e4f4ee484',NULL,NULL,NULL,NULL),('mail.default_content_type','text/plain','Content type to append to the mail messages','f5830019-a60a-47fe-865f-78fb98b3f5f1',NULL,NULL,NULL,NULL),('mail.from','info@openmrs.org','Email address to use as the default from address','a8303d1e-de7b-45b8-bc04-2874ae7da635',NULL,NULL,NULL,NULL),('mail.password','test','Password for the SMTP user (if smtp_auth is enabled)','858118bf-794b-42c4-8266-dcf850963b34',NULL,NULL,NULL,NULL),('mail.smtp_auth','false','true/false whether the smtp host requires authentication','ebe1af62-5e72-4c44-a044-955a9a8b29b4',NULL,NULL,NULL,NULL),('mail.smtp_host','localhost','SMTP host name','ea0625e2-9f64-40a6-9c32-6720772fd3d4',NULL,NULL,NULL,NULL),('mail.smtp_port','25','SMTP port','e77ee4de-13f1-4a7f-b41e-eb2f720cd49d',NULL,NULL,NULL,NULL),('mail.transport_protocol','smtp','Transport protocol for the messaging engine. Valid values: smtp','9fa39a09-2b7a-415c-877c-2308cb3caa12',NULL,NULL,NULL,NULL),('mail.user','test','Username of the SMTP user (if smtp_auth is enabled)','e0f2afbe-eff3-4787-960c-e61b56c0b00d',NULL,NULL,NULL,NULL),('minSearchCharacters','3','Number of characters user must input before searching is started.','3cf7f948-6543-446f-bf62-ed72789bef00',NULL,NULL,NULL,NULL),('module_repository_folder','modules','Name of the folder in which to store the modules','b2d12eb8-a129-43b3-9dd9-dc5460fee0bf',NULL,NULL,NULL,NULL),('newPatientForm.relationships',NULL,'Comma separated list of the RelationshipTypes to show on the new/short patient form.  The list is defined like \'3a, 4b, 7a\'.  The number is the RelationshipTypeId and the \'a\' vs \'b\' part is which side of the relationship is filled in by the user.','08136389-c093-49da-8e9a-c08121e07696',NULL,NULL,NULL,NULL),('new_patient_form.showRelationships','false','true/false whether or not to show the relationship editor on the addPatient.htm screen','63af8912-caaa-4500-b212-8c7c0cb70e6a','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('obs.complex_obs_dir','complex_obs','Default directory for storing complex obs.','4928b05f-2b64-4ea1-9947-245ff4825ca2',NULL,NULL,NULL,NULL),('patient.defaultPatientIdentifierValidator','org.openmrs.patient.impl.LuhnIdentifierValidator','This property sets the default patient identifier validator.  The default validator is only used in a handful of (mostly legacy) instances.  For example, it\'s used to generate the isValidCheckDigit calculated column and to append the string \"(default)\" to the name of the default validator on the editPatientIdentifierType form.','2fd23ac4-60f6-4ae3-9508-9c3f15d77e09',NULL,NULL,NULL,NULL),('patient.headerAttributeTypes',NULL,'A comma delimited list of PersonAttributeType names that will be shown on the patient dashboard','6c1c3e59-8576-43f2-9799-ed92752d8555',NULL,NULL,NULL,NULL),('patient.identifierPrefix',NULL,'This property is only used if patient.identifierRegex is empty.  The string here is prepended to the sql indentifier search string.  The sql becomes \"... where identifier like \'<PREFIX><QUERY STRING><SUFFIX>\';\".  Typically this value is either a percent sign (%) or empty.','6b578bf3-507c-43ed-b0aa-9ccd47a261b4',NULL,NULL,NULL,NULL),('patient.identifierRegex',NULL,'WARNING: Using this search property can cause a drop in mysql performance with large patient sets.  A MySQL regular expression for the patient identifier search strings.  The @SEARCH@ string is replaced at runtime with the user\'s search string.  An empty regex will cause a simply \'like\' sql search to be used. Example: ^0*@SEARCH@([A-Z]+-[0-9])?$','3b223d05-8864-4ef6-9ca9-212483a9645d',NULL,NULL,NULL,NULL),('patient.identifierSearchPattern',NULL,'If this is empty, the regex or suffix/prefix search is used.  Comma separated list of identifiers to check.  Allows for faster searching of multiple options rather than the slow regex. e.g. @SEARCH@,0@SEARCH@,@SEARCH-1@-@CHECKDIGIT@,0@SEARCH-1@-@CHECKDIGIT@ would turn a request for \"4127\" into a search for \"in (\'4127\',\'04127\',\'412-7\',\'0412-7\')\"','98307f49-4ff2-4380-8e5e-d92794bdf4cd',NULL,NULL,NULL,NULL),('patient.identifierSuffix',NULL,'This property is only used if patient.identifierRegex is empty.  The string here is prepended to the sql indentifier search string.  The sql becomes \"... where identifier like \'<PREFIX><QUERY STRING><SUFFIX>\';\".  Typically this value is either a percent sign (%) or empty.','1ff0ff33-44c3-41b2-9111-6b7d336b1581',NULL,NULL,NULL,NULL),('patient.listingAttributeTypes',NULL,'A comma delimited list of PersonAttributeType names that should be displayed for patients in _lists_','b6603753-e0cd-45c8-9d78-e61b49a7efdd',NULL,NULL,NULL,NULL),('patient.nameValidationRegex','^[a-zA-Z \\-]+$','Names of the patients must pass this regex. Eg : ^[a-zA-Z \\-]+$ contains only english alphabet letters, spaces, and hyphens. A value of .* or the empty string means no validation is done.','17584323-1830-4059-a6ae-7aa8a34d9e50',NULL,NULL,NULL,NULL),('patient.viewingAttributeTypes',NULL,'A comma delimited list of PersonAttributeType names that should be displayed for patients when _viewing individually_','1fad0bd5-8990-45a0-b1ea-b473672c4142',NULL,NULL,NULL,NULL),('patientSearch.matchMode','START','Specifies how patient names are matched while searching patient. Valid values are \'ANYWHERE\' or \'START\'. Defaults to start if missing or invalid value is present.','1d59658c-6421-4e40-aba8-246e843a78fd',NULL,NULL,NULL,NULL),('patient_identifier.importantTypes',NULL,'A comma delimited list of PatientIdentifier names : PatientIdentifier locations that will be displayed on the patient dashboard.  E.g.: TRACnet ID:Rwanda,ELDID:Kenya','f7169262-2dee-4e33-b344-8ce24e9e57c9',NULL,NULL,NULL,NULL),('person.searchMaxResults','1000','The maximum number of results returned by patient searches','be882758-29f6-40ae-8755-45312ec9a976',NULL,NULL,NULL,NULL),('report.xmlMacros',NULL,'Macros that will be applied to Report Schema XMLs when they are interpreted. This should be java.util.properties format.','dc59defa-9dd1-4232-90ae-0e577f1fd61f',NULL,NULL,NULL,NULL),('reportProblem.url','http://errors.openmrs.org/scrap','The openmrs url where to submit bug reports','9d004ee7-31dc-4a39-a32e-9ce497f53f01',NULL,NULL,NULL,NULL),('scheduler.password','test','Password for the OpenMRS user that will perform the scheduler activities','05c6367e-64cb-47e9-82d3-f9209bb18ede',NULL,NULL,NULL,NULL),('scheduler.username','admin','Username for the OpenMRS user that will perform the scheduler activities','3cd95dc8-7d76-4267-a8be-6e14387461d3',NULL,NULL,NULL,NULL),('search.caseSensitiveDatabaseStringComparison','true','Indicates whether database string comparison is case sensitive or not. Setting this to false for MySQL with a case insensitive collation improves search performance.','63d90362-4500-4380-9507-fbb878014656',NULL,NULL,NULL,NULL),('searchWidget.batchSize','200','The maximum number of search results that are returned by an ajax call','d5ae5616-4e21-4bc9-8722-d531da74a1b4',NULL,NULL,NULL,NULL),('searchWidget.maximumResults','2000','Specifies the maximum number of results to return from a single search in the search widgets','26c56604-cf39-42b2-8be3-269ced77b8a8',NULL,NULL,NULL,NULL),('searchWidget.runInSerialMode','false','Specifies whether the search widgets should make ajax requests in serial or parallel order, a value of true is appropriate for implementations running on a slow network connection and vice versa','1938204f-b352-4b63-b398-b0d3afb69035','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('searchWidget.searchDelayInterval','400','Specifies time interval in milliseconds when searching, between keyboard keyup event and triggering the search off, should be higher if most users are slow when typing so as to minimise the load on the server','91ac48d0-37e4-4749-bcfc-e824e56a623c',NULL,NULL,NULL,NULL),('security.passwordCannotMatchUsername','true','Configure whether passwords must not match user\'s username or system id','76cfa33c-7dc7-47b3-a94a-81fc48f4e381','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('security.passwordCustomRegex',NULL,'Configure a custom regular expression that a password must match','82813b08-93f5-47fd-b692-ed13bd14649a',NULL,NULL,NULL,NULL),('security.passwordMinimumLength','8','Configure the minimum length required of all passwords','881dabb8-7771-42d9-811e-8522bace053c',NULL,NULL,NULL,NULL),('security.passwordRequiresDigit','true','Configure whether passwords must contain at least one digit','7c8d4bed-ce26-4d94-9a92-057421faa075','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('security.passwordRequiresNonDigit','true','Configure whether passwords must contain at least one non-digit','3159818d-bbaa-44d7-8d0d-2ab0dc4d096c','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('security.passwordRequiresUpperAndLowerCase','true','Configure whether passwords must contain both upper and lower case characters','21b14091-c640-41d4-8a6f-a8517884c0cc','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('user.headerAttributeTypes',NULL,'A comma delimited list of PersonAttributeType names that will be shown on the user dashboard. (not used in v1.5)','d0fecc90-8778-49db-9b4e-91904de31d23',NULL,NULL,NULL,NULL),('user.listingAttributeTypes',NULL,'A comma delimited list of PersonAttributeType names that should be displayed for users in _lists_','63245d29-3cef-479d-9d73-f05b3da47a2f',NULL,NULL,NULL,NULL),('user.requireEmailAsUsername','false','Indicates whether a username must be a valid e-mail or not.','540381e5-3afa-45ad-8c2a-0ff633144bcb','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('user.viewingAttributeTypes',NULL,'A comma delimited list of PersonAttributeType names that should be displayed for users when _viewing individually_','227c8088-9abb-4733-b7ad-3c22aead471c',NULL,NULL,NULL,NULL),('use_patient_attribute.healthCenter','false','Indicates whether or not the \'health center\' attribute is shown when viewing/searching for patients','9927c961-e6b9-40d9-9336-6fcfa0c6c86a','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('use_patient_attribute.mothersName','false','Indicates whether or not mother\'s name is able to be added/viewed for a patient','c45dcb53-fc73-4cbf-9ec7-08d74f86d539','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('visits.allowOverlappingVisits','true','true/false whether or not to allow visits of a given patient to overlap','36e81cc3-fa37-4d77-a428-8d5835ae9a72','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('visits.assignmentHandler','org.openmrs.api.handler.ExistingVisitAssignmentHandler','Set to the name of the class responsible for assigning encounters to visits.','2c62d60f-81cf-4f60-85b4-0b424c241b31',NULL,NULL,NULL,NULL),('visits.autoCloseVisitType',NULL,'comma-separated list of the visit type(s) to automatically close','58736730-6f18-4c6f-8d0e-9baf5a102f39',NULL,NULL,NULL,NULL),('visits.enabled','true','Set to true to enable the Visits feature. This will replace the \'Encounters\' tab with a \'Visits\' tab on the dashboard.','e7259c7d-94a1-4b38-a6a4-4f59e1f7e9e1','org.openmrs.customdatatype.datatype.BooleanDatatype',NULL,NULL,NULL),('visits.encounterTypeToVisitTypeMapping',NULL,'Specifies how encounter types are mapped to visit types when automatically assigning encounters to visits. e.g 1:1, 2:1, 3:2 in the format encounterTypeId:visitTypeId','76333113-1e10-410e-9537-595110c6ce2f',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `global_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hl7_in_archive`
--

DROP TABLE IF EXISTS `hl7_in_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hl7_in_archive` (
  `hl7_in_archive_id` int(11) NOT NULL AUTO_INCREMENT,
  `hl7_source` int(11) NOT NULL DEFAULT '0',
  `hl7_source_key` varchar(255) DEFAULT NULL,
  `hl7_data` text NOT NULL,
  `date_created` datetime NOT NULL,
  `message_state` int(11) DEFAULT '2',
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`hl7_in_archive_id`),
  UNIQUE KEY `hl7_in_archive_uuid_index` (`uuid`),
  KEY `hl7_in_archive_message_state_idx` (`message_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hl7_in_archive`
--

LOCK TABLES `hl7_in_archive` WRITE;
/*!40000 ALTER TABLE `hl7_in_archive` DISABLE KEYS */;
/*!40000 ALTER TABLE `hl7_in_archive` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hl7_in_error`
--

DROP TABLE IF EXISTS `hl7_in_error`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hl7_in_error` (
  `hl7_in_error_id` int(11) NOT NULL AUTO_INCREMENT,
  `hl7_source` int(11) NOT NULL DEFAULT '0',
  `hl7_source_key` text,
  `hl7_data` text NOT NULL,
  `error` varchar(255) NOT NULL DEFAULT '',
  `error_details` mediumtext,
  `date_created` datetime NOT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`hl7_in_error_id`),
  UNIQUE KEY `hl7_in_error_uuid_index` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hl7_in_error`
--

LOCK TABLES `hl7_in_error` WRITE;
/*!40000 ALTER TABLE `hl7_in_error` DISABLE KEYS */;
/*!40000 ALTER TABLE `hl7_in_error` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hl7_in_queue`
--

DROP TABLE IF EXISTS `hl7_in_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hl7_in_queue` (
  `hl7_in_queue_id` int(11) NOT NULL AUTO_INCREMENT,
  `hl7_source` int(11) NOT NULL DEFAULT '0',
  `hl7_source_key` text,
  `hl7_data` text NOT NULL,
  `message_state` int(11) NOT NULL DEFAULT '0',
  `date_processed` datetime DEFAULT NULL,
  `error_msg` text,
  `date_created` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`hl7_in_queue_id`),
  UNIQUE KEY `hl7_in_queue_uuid_index` (`uuid`),
  KEY `hl7_source_with_queue` (`hl7_source`),
  CONSTRAINT `hl7_source_with_queue` FOREIGN KEY (`hl7_source`) REFERENCES `hl7_source` (`hl7_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hl7_in_queue`
--

LOCK TABLES `hl7_in_queue` WRITE;
/*!40000 ALTER TABLE `hl7_in_queue` DISABLE KEYS */;
/*!40000 ALTER TABLE `hl7_in_queue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hl7_source`
--

DROP TABLE IF EXISTS `hl7_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hl7_source` (
  `hl7_source_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` text,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`hl7_source_id`),
  UNIQUE KEY `hl7_source_uuid_index` (`uuid`),
  KEY `user_who_created_hl7_source` (`creator`),
  CONSTRAINT `user_who_created_hl7_source` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hl7_source`
--

LOCK TABLES `hl7_source` WRITE;
/*!40000 ALTER TABLE `hl7_source` DISABLE KEYS */;
INSERT INTO `hl7_source` VALUES (1,'LOCAL','',1,'2006-09-01 00:00:00','8d6b8bb6-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `hl7_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `liquibasechangelog`
--

DROP TABLE IF EXISTS `liquibasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `liquibasechangelog` (
  `ID` varchar(63) NOT NULL,
  `AUTHOR` varchar(63) NOT NULL,
  `FILENAME` varchar(200) NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int(11) NOT NULL,
  `EXECTYPE` varchar(10) NOT NULL,
  `MD5SUM` varchar(35) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `COMMENTS` varchar(255) DEFAULT NULL,
  `TAG` varchar(255) DEFAULT NULL,
  `LIQUIBASE` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`ID`,`AUTHOR`,`FILENAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `liquibasechangelog`
--

LOCK TABLES `liquibasechangelog` WRITE;
/*!40000 ALTER TABLE `liquibasechangelog` DISABLE KEYS */;
INSERT INTO `liquibasechangelog` VALUES ('0','bwolfe','liquibase-update-to-latest.xml','2011-09-20 00:00:00',10016,'MARK_RAN','3:ccc4741ff492cb385f44e714053920af',NULL,NULL,NULL,NULL),('02232009-1141','nribeka','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10061,'EXECUTED','3:b5921fb42deb90fe52e042838d0638a0','Modify Column','Modify the password column to fit the output of SHA-512 function',NULL,'2.0.5'),('1','upul','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10042,'MARK_RAN','3:7fbc03c45bb69cd497b096629d32c3f5','Add Column','Add the column to person_attribute type to connect each type to a privilege',NULL,'2.0.5'),('1226348923233-12','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:28',10022,'EXECUTED','3:7efb7ed5267126e1e44c9f344e35dd7d','Insert Row (x12)','',NULL,'2.0.5'),('1226348923233-13','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:28',10023,'EXECUTED','3:8b9e14aa00a4382aa2623b39400c9110','Insert Row (x2)','',NULL,'2.0.5'),('1226348923233-14','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10027,'EXECUTED','3:8910082a3b369438f86025e4006b7538','Insert Row (x4)','',NULL,'2.0.5'),('1226348923233-15','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10028,'EXECUTED','3:8485e0ebef4dc368ab6b87de939f8e82','Insert Row (x15)','',NULL,'2.0.5'),('1226348923233-16','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:28',10019,'EXECUTED','3:5778f109b607f882cc274750590d5004','Insert Row','',NULL,'2.0.5'),('1226348923233-17','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10030,'EXECUTED','3:3c324233bf1f386dcc4a9be55401c260','Insert Row (x2)','',NULL,'2.0.5'),('1226348923233-18','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10031,'EXECUTED','3:40ad1a506929811955f4d7d4753d576e','Insert Row (x2)','',NULL,'2.0.5'),('1226348923233-2','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:28',10020,'EXECUTED','3:35613fc962f41ed143c46e578fd64a70','Insert Row (x5)','',NULL,'2.0.5'),('1226348923233-20','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10032,'EXECUTED','3:0ce5c5b83b4754b44f4bcda8eb866f3a','Insert Row','',NULL,'2.0.5'),('1226348923233-21','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10033,'EXECUTED','3:51c90534135f429c1bcde82be0f6157d','Insert Row','',NULL,'2.0.5'),('1226348923233-22','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:28',10018,'EXECUTED','3:2d4897a84ce4408d8fcec69767a5c563','Insert Row','',NULL,'2.0.5'),('1226348923233-23','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10034,'EXECUTED','3:19f78a07a33a5efc28b4712a07b02a29','Insert Row','',NULL,'2.0.5'),('1226348923233-6','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10026,'EXECUTED','3:a947f43a1881ac56186039709a4a0ac8','Insert Row (x13)','',NULL,'2.0.5'),('1226348923233-8','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:28',10021,'EXECUTED','3:dceb0cc19be3545af8639db55785d66e','Insert Row (x7)','',NULL,'2.0.5'),('1226412230538-24','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:28',10024,'EXECUTED','3:0b77e92c0d1482c1bef7ca1add6b233b','Insert Row (x2)','',NULL,'2.0.5'),('1226412230538-7','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10025,'EXECUTED','3:c189f41d824649ef72dc3cef74d3580b','Insert Row (x106)','',NULL,'2.0.5'),('1226412230538-9a','ben (generated)','liquibase-core-data.xml','2016-07-07 08:14:29',10035,'EXECUTED','3:73c2b426be208fb50f088ad4ee76c8d6','Insert Row (x4)','',NULL,'2.0.5'),('1227123456789-100','dkayiwa','liquibase-schema-only.xml','2016-07-07 08:14:24',178,'EXECUTED','3:24751e1218f5fff3d2abf8e281e557c5','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-1','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',1,'EXECUTED','3:a851046bb3eb5b0daccb6e69ef8a9a00','Create Table','',NULL,'2.0.5'),('1227303685425-10','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',8,'EXECUTED','3:3fb7e78555ddf8d8014ba336bb8b5402','Create Table','',NULL,'2.0.5'),('1227303685425-100','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',114,'EXECUTED','3:8d20fc37ce4266cba349eeef66951688','Create Index','',NULL,'2.0.5'),('1227303685425-101','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',115,'EXECUTED','3:cc9b2ad0c2ff9ad6fcfd2f56b52d795f','Create Index','',NULL,'2.0.5'),('1227303685425-102','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',116,'EXECUTED','3:97d1301e8ab7f35e109c733fdedde10f','Create Index','',NULL,'2.0.5'),('1227303685425-103','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',117,'EXECUTED','3:2447e4abc7501a18f401594e4c836fff','Create Index','',NULL,'2.0.5'),('1227303685425-104','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',118,'EXECUTED','3:8d6c644eaf9f696e3fee1362863c26ec','Create Index','',NULL,'2.0.5'),('1227303685425-105','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',119,'EXECUTED','3:fb3838f818387718d9b4cbf410d653cd','Create Index','',NULL,'2.0.5'),('1227303685425-106','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',120,'EXECUTED','3:a644de1082a85ab7a0fc520bb8fc23d7','Create Index','',NULL,'2.0.5'),('1227303685425-107','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',121,'EXECUTED','3:f11eb4e4bc4a5192b7e52622965aacb2','Create Index','',NULL,'2.0.5'),('1227303685425-108','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',135,'EXECUTED','3:07fc6fd2c0086f941aed0b2c95c89dc8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-109','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',136,'EXECUTED','3:c2911be31587bbc868a55f13fcc3ba5e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-11','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',9,'EXECUTED','3:1cef06ece4f56bfbe205ec03b75a129f','Create Table','',NULL,'2.0.5'),('1227303685425-110','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',137,'EXECUTED','3:32c42fa39fe81932aa02974bb19567ed','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-111','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',138,'EXECUTED','3:f35c8159ca7f84ae551bdb988b833760','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-112','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',139,'EXECUTED','3:df0a45bc276e7484f183e3190cff8394','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-115','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',140,'EXECUTED','3:d3b13502ef9794718d68bd0697fd7c2b','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-116','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',141,'EXECUTED','3:6014d91cadbbfc05bd364619d94a4f18','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-117','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',142,'EXECUTED','3:0841471be0ebff9aba768017b9a9717b','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-118','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',143,'EXECUTED','3:c73351f905761c3cee7235b526eff1a0','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-119','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',144,'EXECUTED','3:cd72c79bfd3c807ba5451d8ca5cb2612','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-12','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',10,'EXECUTED','3:b9726f1c78d0cba40d5ee61e721350f7','Create Table','',NULL,'2.0.5'),('1227303685425-120','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',145,'EXECUTED','3:b07d718d9d2b64060584d4c460ffc277','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-121','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',146,'EXECUTED','3:be141d71df248fba87a322b35f13b4db','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-122','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',147,'EXECUTED','3:7bcc45dda3aeea4ab3916701483443d3','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-123','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',148,'EXECUTED','3:031e4dcf20174b92bbbb07323b86d569','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-124','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',149,'EXECUTED','3:7d277181a4e9d5e14f9cb1220c6c4c57','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-125','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',150,'EXECUTED','3:7172ef61a904cd7ae765f0205d9e66dd','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-126','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',151,'EXECUTED','3:0d9a3ffc816c3e4e8649df3de01a8ff6','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-128','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',152,'EXECUTED','3:3a9357d6283b2bb97c1423825d6d57eb','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-129','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',153,'EXECUTED','3:e3923913d6f34e0e8bc7333834884419','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-13','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',11,'EXECUTED','3:982544aff0ae869f5ac9691d5c93a7e4','Create Table','',NULL,'2.0.5'),('1227303685425-130','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',154,'EXECUTED','3:dae7a98f3643acfe9db5c3b4b9e8f4ea','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-131','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',155,'EXECUTED','3:44a4e4791a0f727fffc96b9dab0a3fa8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-132','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',156,'EXECUTED','3:16dcc5a95708dbdaff07ed27507d8e29','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-134','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',157,'EXECUTED','3:c37757ed38ace0bb94d8455a49e3049a','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-135','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',158,'EXECUTED','3:ab40ab94ab2f86a0013ecbf9dd034de4','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-136','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',159,'EXECUTED','3:3878ab735b369d778a7feb2b92746352','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-137','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',160,'EXECUTED','3:a7bf99f775c2f07b534a4df4e5c5c20b','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-139','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',161,'EXECUTED','3:f0a1690648292d939876bdeefa74792a','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-14','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',12,'EXECUTED','3:0132a13f3ff3c212ad7e11a9a0890bbb','Create Table','',NULL,'2.0.5'),('1227303685425-140','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',162,'EXECUTED','3:7ba4860a1e0a00ff49a93d4e86929691','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-141','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',163,'EXECUTED','3:5b176976d808cf8b1b8fae7d2b19e059','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-142','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',164,'EXECUTED','3:5538db250e63d70a79dce2c5a74ee528','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-143','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',165,'EXECUTED','3:a981ac9be845bf6c6098aa98cd4d8456','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-144','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',166,'EXECUTED','3:84428d9dce773758f73616129935d888','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-145','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',167,'EXECUTED','3:6b8af6c242f1d598591478897feed2d8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-146','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',168,'EXECUTED','3:a8cddf3b63050686248e82a3b6de781f','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-147','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',169,'EXECUTED','3:e8b5350ad40fa006c088f08fae4d3141','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-149','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',170,'EXECUTED','3:b6c44ee5824ae261a9a87b8ac60fe23d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-15','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',13,'EXECUTED','3:18dd3e507ecbe5212bcdf0a0a8012d88','Create Table','',NULL,'2.0.5'),('1227303685425-150','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',171,'EXECUTED','3:f8c495d78d68c9fc701271a8e5d1f102','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-151','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',172,'EXECUTED','3:94dc5b8d27f275fb06cc230eb313e430','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-153','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',173,'EXECUTED','3:2a04930665fe64516765263d1a9b0775','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-154','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',174,'EXECUTED','3:adfef8b8dd7b774b268b0968b7400f42','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-155','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',175,'EXECUTED','3:540b0422c733b464a33ca937348d8b4c','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-158','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',176,'EXECUTED','3:d1d73e19bab5821f256c01a83e2d945f','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-159','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',177,'EXECUTED','3:c23d0cd0eec5f20385b4182af18fc835','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-16','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',14,'EXECUTED','3:4c19b5c980b58e54af005e1fa50359ae','Create Table','',NULL,'2.0.5'),('1227303685425-160','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',179,'EXECUTED','3:cc64f6e676cb6a448f73599d8149490c','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-161','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',180,'EXECUTED','3:f48e300a3439d90fa2d518b3d6e145a5','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-162','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',181,'EXECUTED','3:467e31995c41be55426a5256d99312c4','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-163','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',182,'EXECUTED','3:45a4519c252f7e42d649292b022ec158','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-164','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',183,'EXECUTED','3:dfd51e701b716c07841c2e4ea6f59f3e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-165','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',184,'EXECUTED','3:0d69b82ce833ea585e95a6887f800108','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-166','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',185,'EXECUTED','3:2070f44c444e1e6efdbe7dfb9f7b846d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-167','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',186,'EXECUTED','3:9196c0f9792007c72233649cc7c2ac58','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-168','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',187,'EXECUTED','3:33d644a49e92a4bbd4cb653d6554c8d0','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-169','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',188,'EXECUTED','3:e75c37cbd9aa22cf95b2dc89fdb2c831','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-17','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',15,'EXECUTED','3:250e3f0d4bb139fa751d0875fd1d5b89','Create Table','',NULL,'2.0.5'),('1227303685425-170','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',189,'EXECUTED','3:ac31515d8822caa0c87705cb0706e52f','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-171','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',190,'EXECUTED','3:b40823e1322acea52497f43033e72e5e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-173','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',191,'EXECUTED','3:b37c0b43a23ad3b072e34055875f7dcc','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-174','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',192,'EXECUTED','3:f8c5737a51f0f040e9fac3060c246e46','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-175','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',193,'EXECUTED','3:87cc15f9622b014d01d4df512a3f835e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-176','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',194,'EXECUTED','3:f57273dfbaba02ea785a0e165994f74b','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-177','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',195,'EXECUTED','3:1555790e99827ded259d5ec7860eb1b1','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-178','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',196,'EXECUTED','3:757206752885a07d1eea5585ad9e2dce','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-179','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',197,'EXECUTED','3:018efd7d7a5e84f7c2c9cec7299d596e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-18','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',16,'EXECUTED','3:2eb2063d3e1233e7ebc23f313da5bff6','Create Table','',NULL,'2.0.5'),('1227303685425-180','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',198,'EXECUTED','3:9ba52b3b7059674e881b7611a3428bde','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-181','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',199,'EXECUTED','3:ffee79a7426d7e41cf65889c2a5064f2','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-182','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',200,'EXECUTED','3:380743d4f027534180d818f5c507fae9','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-183','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',201,'EXECUTED','3:6882a4cf798e257af34753a8b5e7a157','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-184','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',202,'EXECUTED','3:93473623db4b6e7ca7813658da5b6771','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-185','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',203,'EXECUTED','3:f19a46800a4695266f3372aa709650b2','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-186','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',204,'EXECUTED','3:adfd7433de8d3b196d1166f62e497f8d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-187','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',205,'EXECUTED','3:88b09239d29fbddd8bf4640df9f3e235','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-188','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',206,'EXECUTED','3:777e5970e09a3a1608bf7c40ef1ea1db','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-189','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',207,'EXECUTED','3:40dab4e434aa06340ba046fbd1382c6d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-19','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',17,'EXECUTED','3:b5acf27abca6fec3533e081a6c57c415','Create Table','',NULL,'2.0.5'),('1227303685425-190','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',208,'EXECUTED','3:8766098ff9779a913d5642862955eaff','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-191','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',209,'EXECUTED','3:2a52afd1df6dcf64ec21f3c6ffe1d022','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-192','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',210,'EXECUTED','3:fa5de48b1490faa157e1977529034169','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-193','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',211,'EXECUTED','3:1213cb90fa9bd1561a371cc53c262d0f','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-194','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',212,'EXECUTED','3:2eb07e7388ff8d68d36cf2f3552c1a7c','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-195','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',213,'EXECUTED','3:d6ec9bb7b3bab333dcea4a3c18083616','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-196','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',214,'EXECUTED','3:df03afd5ef34e472fd6d43ef74a859e1','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-197','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',215,'EXECUTED','3:a0811395501a4423ca66de08fcf53895','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-198','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',216,'EXECUTED','3:e46a78b95280d9082557ed991af8dbe7','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-199','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:24',217,'EXECUTED','3:e57afffae0d6a439927e45cde4393363','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-2','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',2,'EXECUTED','3:d90246bb4d8342608e818a872d3335f1','Create Table','',NULL,'2.0.5'),('1227303685425-20','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',18,'EXECUTED','3:e0a8a4978c536423320f1ff44520169a','Create Table','',NULL,'2.0.5'),('1227303685425-200','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',218,'EXECUTED','3:667c2308fcf366f47fab8d9df3a3b2ae','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-201','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',219,'EXECUTED','3:104750a2b7779fa43e8457071e0bc33e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-202','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',220,'EXECUTED','3:4b813b03362a54d89a28ed1b10bc9069','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-203','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',221,'EXECUTED','3:526893ceedd67d8a26747e314a15f501','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-204','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',222,'EXECUTED','3:d0dbb7cc972e73f6a429b273ef63132e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-205','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',223,'EXECUTED','3:44244a3065d9a5531a081d176aa4e93d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-207','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',224,'EXECUTED','3:77ade071c48615dbb39cbf9f01610c0e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-208','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',225,'EXECUTED','3:04495bf48c23d0fe56133da87c4e9a66','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-209','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',226,'EXECUTED','3:ff99d75d98ce0428a57100aeb558a529','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-21','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',19,'EXECUTED','3:f2353036e6382f45f91af5d8024fb04c','Create Table','',NULL,'2.0.5'),('1227303685425-210','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',227,'EXECUTED','3:537b2e8f88277a6276bcdac5d1493e4e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-211','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',228,'EXECUTED','3:3da39190692480b0a610b5c66fd056b8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-212','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',229,'EXECUTED','3:347f20b32a463b73f0a93de13731a3a3','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-213','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',230,'EXECUTED','3:b89ee7f6dd678737268566b7e7d0d5d3','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-214','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',231,'EXECUTED','3:5f4b3400ecb50d46e04a18b6b57821c8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-215','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',232,'EXECUTED','3:3dfa6664ca6b77eee492af73908f7312','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-216','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',233,'EXECUTED','3:7f7dbdcdaf3914e33458c0d67bc326db','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-217','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',234,'EXECUTED','3:1f03c97bd9b3ee1c2726656c6a0db795','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-218','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',235,'EXECUTED','3:e0a67bb4f3ea4b44de76fd73ca02ddb3','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-219','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',236,'EXECUTED','3:9a347c93be3356b84358ada2264ed201','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-22','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',20,'EXECUTED','3:5ff0caa8c8497fd681f111bf2842baca','Create Table','',NULL,'2.0.5'),('1227303685425-220','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',237,'EXECUTED','3:8a744c0020e6c6ae519ed0a04d79f82d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-221','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',238,'EXECUTED','3:cc3f5b38ea88221efe32bc99be062edf','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-222','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',239,'EXECUTED','3:a90ffe3cbe9ddd1704e702e71ba5a216','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-223','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',240,'EXECUTED','3:ee4b79223897197c46c79d6ed2e68538','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-224','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',241,'EXECUTED','3:56d7625e53d13008ae7a31d09ba7dab8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-225','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',242,'EXECUTED','3:b94477e4e6ecf22bc973408d2d01a868','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-226','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',243,'EXECUTED','3:d7a0cde832f1f557f0f42710645c1b50','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-227','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',244,'EXECUTED','3:6873a8454254a783dbcbe608828c0bd0','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-228','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',245,'EXECUTED','3:6ffcdbbe70b8f8e096785a3c2fe83318','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-229','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',246,'EXECUTED','3:ef5d7095407e0df6a1fcaf7c3c55872b','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-23','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',21,'EXECUTED','3:c3aa4ad35ead35e805a99083a95a1c86','Create Table','',NULL,'2.0.5'),('1227303685425-230','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',247,'EXECUTED','3:306710c2acba2e689bf1121d577f449b','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-231','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',248,'EXECUTED','3:71f320edc9221ce73876d80077b7b94d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-232','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',249,'EXECUTED','3:8dfba47fb6719dc743b231cc645a8378','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-234','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',250,'EXECUTED','3:9b12808a0fe62d6951bcd61f9cbff3f8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-235','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',251,'EXECUTED','3:a3e9822b106a9bb42f5b9d28dc70335c','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-236','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',252,'EXECUTED','3:388be0f658f8bf6df800fe3efd4dadb3','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-237','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',253,'EXECUTED','3:eee4cb65598835838fd6deb8e4043693','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-239','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',254,'EXECUTED','3:3b38e45410dd1d02530d012a12b6b03c','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-24','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',22,'EXECUTED','3:f4f4e3a5fa3d93bb50d2004c6976cc12','Create Table','',NULL,'2.0.5'),('1227303685425-240','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',255,'EXECUTED','3:095316f05dac21b4a33a141e5781d99d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-241','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',256,'EXECUTED','3:2cffae7a53d76f19e5194778cff75a4f','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-242','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',257,'EXECUTED','3:828732619d67fa932631e18827d74463','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-243','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',258,'EXECUTED','3:730166a1b0c3162e8ce882e0c8f308c5','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-244','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',259,'EXECUTED','3:77c03c05576961f7efebdfa10ae68119','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-245','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',260,'EXECUTED','3:d7d8dcaceb9793b0801c87eb2c94cd11','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-246','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',261,'EXECUTED','3:d395ea3ef18817dc23e750a1048cb4e1','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-247','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',262,'EXECUTED','3:4546bff7866082946f19e5d82ffc4d2e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-248','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',263,'EXECUTED','3:4e1071a7c1047f2d3b49778ce2f8bc40','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-249','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',264,'EXECUTED','3:fd6b7823929af9fded1f213d319eae13','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-25','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',23,'EXECUTED','3:2a720a7c4302c435c06c045abcce3b4d','Create Table','',NULL,'2.0.5'),('1227303685425-250','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',265,'EXECUTED','3:955129e5e6adf3723583c047eb33583d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-251','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',266,'EXECUTED','3:7a12c926e69a3d1a7e31da2b8d7123e5','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-252','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',267,'EXECUTED','3:eb8c61b5b792346af3d3f8732278260b','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-253','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',268,'EXECUTED','3:761e6c7fb13a82c6ab671039e5dc5646','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-254','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',269,'EXECUTED','3:9a2401574c95120e1f90d18fde428d10','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-255','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',270,'EXECUTED','3:cda3d0c5c91b85d9f4610554eabb331e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-256','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',271,'EXECUTED','3:ebf6243c66261bf0168e72ceccd0fdb8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-257','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',272,'EXECUTED','3:8dc087b963a10a52a22312c3c995cec2','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-258','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',273,'EXECUTED','3:489d5e366070f6b2424b8e5a20d0118f','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-259','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',274,'EXECUTED','3:d8d2a1cfddf07123a8e6f52b1e71705d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-26','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',24,'EXECUTED','3:8adbeeca3dce3d9befb7a02714d20fec','Create Table','',NULL,'2.0.5'),('1227303685425-260','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',275,'EXECUTED','3:741f0c9e309b8515b713decb56ed6cb2','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-261','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:25',276,'EXECUTED','3:7034f7db7864956b7ca13ceb70cc8a92','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-262','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',277,'EXECUTED','3:e3a5995253a29723231b0912b971fb5a','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-263','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',278,'EXECUTED','3:b1d3718c15765d4a3bf89cb61376d3af','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-264','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',279,'EXECUTED','3:973683323e2ce886f07ef53a6836ad1e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-265','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',280,'EXECUTED','3:eff44c0cd530b852864042134ebccb47','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-266','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',281,'EXECUTED','3:2493248589e21293811c01cdb6c2fb87','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-267','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',282,'EXECUTED','3:ee636bdbc5839d7de0914648e1f07431','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-268','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',283,'EXECUTED','3:96710f10538b24f39e74ebc13eb6a3fc','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-269','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',284,'EXECUTED','3:5be60bacdaf2ab2d8a3103e36b32f6b9','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-27','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',25,'EXECUTED','3:6d359a7595e5b9a61ac1101aa7df59e7','Create Table','',NULL,'2.0.5'),('1227303685425-270','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',285,'EXECUTED','3:35a1f2d06c31af2f02df3e7aea4d05a5','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-271','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',286,'EXECUTED','3:64d94dfba329b70a842a09b01b952850','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-272','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',287,'EXECUTED','3:0e3787e31b95815106e7e051b9c4a79a','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-273','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',288,'EXECUTED','3:00949a7bd184ed4ee994eabf4b98a41f','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-274','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',289,'EXECUTED','3:b63e3786d661815d2b7c63b277796fc9','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-275','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',290,'EXECUTED','3:7e0ac267990b953ff9efe8fece53b4dd','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-276','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',291,'EXECUTED','3:d66f7691a19406c215b3b4b4c5330775','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-277','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',292,'EXECUTED','3:9b7c8b6ab0b9f8ffde5e7853efa40db5','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-278','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',293,'EXECUTED','3:e5f95724ac551e5905c604e59af444f9','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-279','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',294,'EXECUTED','3:6b4a4c9072a92897562aa595c27aaae4','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-28','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',26,'EXECUTED','3:7264098c8ccfa42b12ffb13056f77383','Create Table','',NULL,'2.0.5'),('1227303685425-280','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',295,'EXECUTED','3:8fb315815532eb73c13fac2dac763f69','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-281','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',296,'EXECUTED','3:1bed6131408c745505800d96130d3b30','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-282','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',297,'EXECUTED','3:ecf4138f4fd2d1e8be720381ac401623','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-283','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',298,'EXECUTED','3:00b414c29dcc3be9683f28ff3f2d9b20','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-284','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',299,'EXECUTED','3:b71dcc206a323ffa6ac4cd658de7b435','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-285','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',300,'EXECUTED','3:17423acbed3db4325d48d91e9f0e7147','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-286','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',301,'EXECUTED','3:2d8576bdbc9dd67137ba462b563022d8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-287','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',302,'EXECUTED','3:6798c27ddf8ca72952030d6005422c1e','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-288','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',303,'EXECUTED','3:b28ec2579454fa7a13fd3896420ad1ff','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-289','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',304,'EXECUTED','3:cbfff99e22305c5570cdc8fdb33f3542','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-29','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',27,'EXECUTED','3:750f7e9a5f0898408626a11ce7f34ee4','Create Table','',NULL,'2.0.5'),('1227303685425-290','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',305,'EXECUTED','3:66957ec2b3211869a1ad777de33e7983','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-291','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',306,'EXECUTED','3:18b7da760f632dc6baf910fe5001212d','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-292','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',307,'EXECUTED','3:a1a914015e07b1637a9c655a9be3cfcd','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-293','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',308,'EXECUTED','3:5fedacb04729210c4a27bbfa2a3704f1','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-294','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',309,'EXECUTED','3:cf53101d520adb79fd1827819bcf0401','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-295','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',310,'EXECUTED','3:22b93c390cd6054f3dc8b62814d143cf','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-296','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',311,'EXECUTED','3:8b71fc2ae6be26a1ddc499cfc6e2cdba','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-297','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',312,'EXECUTED','3:d10fb06a37b1433a248b549ebae31e63','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-298','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',313,'EXECUTED','3:a3008458deed3c8c95f475395df6d788','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-299','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',314,'EXECUTED','3:b380ee7cce1b82a2f983d242b45c63b3','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-30','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',28,'EXECUTED','3:0951dbc13f8f4c6fd4ccc8a7ddb9d77c','Create Table','',NULL,'2.0.5'),('1227303685425-300','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',315,'EXECUTED','3:01f02c28d4f52e712aad87873aaa40f8','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-301','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:26',316,'EXECUTED','3:adf03ccc09e8f37f827b8ffbf3afff83','Add Foreign Key Constraint','',NULL,'2.0.5'),('1227303685425-31','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',29,'EXECUTED','3:5669760a2908489a63a69c3d34dd5c54','Create Table','',NULL,'2.0.5'),('1227303685425-32','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',30,'EXECUTED','3:c8b2b1bb1eb7b3885c89f436210cc2d5','Create Table','',NULL,'2.0.5'),('1227303685425-33','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',31,'EXECUTED','3:7b55b4b34bca59cf0d0b7271a2906568','Create Table','',NULL,'2.0.5'),('1227303685425-34','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',32,'EXECUTED','3:05a6b514927868471d71b334502d0e85','Create Table','',NULL,'2.0.5'),('1227303685425-35','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',33,'EXECUTED','3:528a3f364b7acce00fcc4d49153a5626','Create Table','',NULL,'2.0.5'),('1227303685425-36','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',34,'EXECUTED','3:2f8eb6548a5d935d7df6f68b329b7685','Create Table','',NULL,'2.0.5'),('1227303685425-37','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',35,'EXECUTED','3:fb11397b44997fe8fef33f8ae86d4044','Create Table','',NULL,'2.0.5'),('1227303685425-39','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',36,'EXECUTED','3:3efa037ecd0227f935b601f0bda28692','Create Table','',NULL,'2.0.5'),('1227303685425-4','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',3,'EXECUTED','3:27c254248b84b163e54161c6f14c2104','Create Table','',NULL,'2.0.5'),('1227303685425-40','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',37,'EXECUTED','3:934d8b580b8b3572b3795a92496783a2','Create Table','',NULL,'2.0.5'),('1227303685425-41','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',38,'EXECUTED','3:a65a25558c348c19863a0088ae031ad7','Create Table','',NULL,'2.0.5'),('1227303685425-42','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',39,'EXECUTED','3:1264d39b6cb1fa81263df8f7a0819a5e','Create Table','',NULL,'2.0.5'),('1227303685425-43','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',40,'EXECUTED','3:bbdbc4ae1631b83db687f4a92453ba3e','Create Table','',NULL,'2.0.5'),('1227303685425-44','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',41,'EXECUTED','3:98d047deb448c37f252eca7c035bf158','Create Table','',NULL,'2.0.5'),('1227303685425-45','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',42,'EXECUTED','3:a8a1d8af033a6e76c1e2060727bf4ebe','Create Table','',NULL,'2.0.5'),('1227303685425-46','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',43,'EXECUTED','3:95fa512a96ab3d1d070d11614c2b5fd5','Create Table','',NULL,'2.0.5'),('1227303685425-47','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',44,'EXECUTED','3:7ae5af2d7f0b3e0bfaf9f243f56851eb','Create Table','',NULL,'2.0.5'),('1227303685425-48','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',45,'EXECUTED','3:fdbf71c7035399d628225af885c63114','Create Table','',NULL,'2.0.5'),('1227303685425-49','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',46,'EXECUTED','3:0c00abd4429f2072f360915eb2eec3de','Create Table','',NULL,'2.0.5'),('1227303685425-5','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',4,'EXECUTED','3:63b45eb5e407f1aa4e6569392ca957ca','Create Table','',NULL,'2.0.5'),('1227303685425-50','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',47,'EXECUTED','3:0e2c0cecd82166846a869f8ecce32bc2','Create Table','',NULL,'2.0.5'),('1227303685425-51','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',48,'EXECUTED','3:308c259886e442254ca616d365db78a2','Create Table','',NULL,'2.0.5'),('1227303685425-52','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',49,'EXECUTED','3:c11b85565b591de38d518fe60411507d','Create Table','',NULL,'2.0.5'),('1227303685425-53','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',50,'EXECUTED','3:2860fbf54a18e959646882325eb4dd87','Create Table','',NULL,'2.0.5'),('1227303685425-54','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',51,'EXECUTED','3:350eb1d5e1f0a2db57a1487790e56769','Create Table','',NULL,'2.0.5'),('1227303685425-55','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',52,'EXECUTED','3:2318a41398b0583575e358aa79813cc6','Create Table','',NULL,'2.0.5'),('1227303685425-56','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',53,'EXECUTED','3:9fcb808d4c751b4d65850479558590d9','Create Table','',NULL,'2.0.5'),('1227303685425-57','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',54,'EXECUTED','3:401395592f65e4216b0b0e8a756ae9b8','Create Table','',NULL,'2.0.5'),('1227303685425-58','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',55,'EXECUTED','3:fcb459a88d234d6556ab5f5aff73a926','Create Table','',NULL,'2.0.5'),('1227303685425-59','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',56,'EXECUTED','3:ab774554467ea7818601acb495c57e5a','Create Table','',NULL,'2.0.5'),('1227303685425-6','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',5,'EXECUTED','3:570a29d5b7f6b945d80de91fce59c0f6','Create Table','',NULL,'2.0.5'),('1227303685425-60','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',57,'EXECUTED','3:54282635bb2bf4218be532ed940ac4b0','Create Table','',NULL,'2.0.5'),('1227303685425-61','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',58,'EXECUTED','3:920daa80f4f9db2cacee8dca6ca4f971','Create Table','',NULL,'2.0.5'),('1227303685425-62','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',59,'EXECUTED','3:55daf6d077eac0ef7e30e6395bc4bc68','Create Table','',NULL,'2.0.5'),('1227303685425-63','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',60,'EXECUTED','3:cdc470c39dadd7cb1a1527a82ff737d3','Create Table','',NULL,'2.0.5'),('1227303685425-64','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',61,'EXECUTED','3:e3eb66044ea03e417837e9c1668f28e3','Create Table','',NULL,'2.0.5'),('1227303685425-65','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',62,'EXECUTED','3:f3a2f7801224c3f3410bc9f7a1cfebff','Create Table','',NULL,'2.0.5'),('1227303685425-66','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',63,'EXECUTED','3:93e2d359d5f6c38b95dfd47dce687c9c','Create Table','',NULL,'2.0.5'),('1227303685425-67','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',64,'EXECUTED','3:ec491d9f71a9e334e005a4f85c9ffdc6','Create Table','',NULL,'2.0.5'),('1227303685425-68','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',65,'EXECUTED','3:40096bd3e62db8377ce4f0a1fcea444e','Create Table','',NULL,'2.0.5'),('1227303685425-7','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',6,'EXECUTED','3:13a412a8d9125f657594bc96f742dd1b','Create Table','',NULL,'2.0.5'),('1227303685425-70','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',66,'EXECUTED','3:0df5ce250df07062c43119d18fc2a85b','Create Table','',NULL,'2.0.5'),('1227303685425-71','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',67,'EXECUTED','3:06e7ba94af07838a3d2ebb98816412a3','Create Table','',NULL,'2.0.5'),('1227303685425-72','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:22',68,'EXECUTED','3:01610d6974df470c653bc34953a31335','Create Table','',NULL,'2.0.5'),('1227303685425-73','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',89,'EXECUTED','3:33d08000805c4b9d7db06556961553b1','Add Primary Key','',NULL,'2.0.5'),('1227303685425-75','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',90,'EXECUTED','3:f2b0a95b4015b54d38c721906abc1fdb','Add Primary Key','',NULL,'2.0.5'),('1227303685425-77','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',91,'EXECUTED','3:bdde9c0d7374a3468a94426199b0d930','Add Primary Key','',NULL,'2.0.5'),('1227303685425-78','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',92,'EXECUTED','3:6fb4014a9a3ecc6ed09a896936b8342d','Add Primary Key','',NULL,'2.0.5'),('1227303685425-79','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',93,'EXECUTED','3:77e1d7c49e104435d10d90cc70e006e3','Add Primary Key','',NULL,'2.0.5'),('1227303685425-81','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',94,'EXECUTED','3:a5871abe4cdc3d8d9390a9b4ab0d0776','Add Primary Key','',NULL,'2.0.5'),('1227303685425-82','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',95,'EXECUTED','3:2f7eab1e485fd5a653af8799a84383b4','Add Primary Key','',NULL,'2.0.5'),('1227303685425-83','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',96,'EXECUTED','3:60ca763d5ac940b3bc189e2f28270bd8','Add Primary Key','',NULL,'2.0.5'),('1227303685425-84','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',97,'EXECUTED','3:901f48ab4c9e3a702fc0b38c5e724a5e','Add Primary Key','',NULL,'2.0.5'),('1227303685425-85','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',99,'EXECUTED','3:5544801862c8f21461acf9a22283ccab','Create Index','',NULL,'2.0.5'),('1227303685425-86','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',100,'EXECUTED','3:70591fc2cd8ce2e7bda36b407bbcaa86','Create Index','',NULL,'2.0.5'),('1227303685425-87','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',101,'EXECUTED','3:35c206a147d28660ffee5f87208f1f6b','Create Index','',NULL,'2.0.5'),('1227303685425-88','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',102,'EXECUTED','3:d399797580e14e7d67c1c40637314476','Create Index','',NULL,'2.0.5'),('1227303685425-89','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',103,'EXECUTED','3:138fa4373fe05e63fe5f923cf3c17e69','Create Index','',NULL,'2.0.5'),('1227303685425-9','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:21',7,'EXECUTED','3:ac6886dbc0c811bda6909908fb2d30a2','Create Table','',NULL,'2.0.5'),('1227303685425-90','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',104,'EXECUTED','3:4b60e13b8e209c2b5b1f981f4c28fc1b','Create Index','',NULL,'2.0.5'),('1227303685425-91','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',105,'EXECUTED','3:f9c13df6f50d1e7c1fad36faa020d7a6','Create Index','',NULL,'2.0.5'),('1227303685425-92','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',106,'EXECUTED','3:c24d9e0d28b3a208dbe2fc1cfaf23720','Create Index','',NULL,'2.0.5'),('1227303685425-93','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',107,'EXECUTED','3:ae9beae273f9502bc01580754e0f2bdf','Create Index','',NULL,'2.0.5'),('1227303685425-94','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',108,'EXECUTED','3:39d98e23d1480b677bc8f2341711756b','Create Index','',NULL,'2.0.5'),('1227303685425-95','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',109,'EXECUTED','3:16ece63cd24c4c5048356cc2854235e1','Create Index','',NULL,'2.0.5'),('1227303685425-96','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',110,'EXECUTED','3:de9943f6a1500bd3f94cb7e0c1d3bde7','Create Index','',NULL,'2.0.5'),('1227303685425-97','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',111,'EXECUTED','3:c0fac38fa4928378abe6f47bd78926b1','Create Index','',NULL,'2.0.5'),('1227303685425-98','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',112,'EXECUTED','3:4c8938f3ea457f5f4f4936e9cbaf898b','Create Index','',NULL,'2.0.5'),('1227303685425-99','ben (generated)','liquibase-schema-only.xml','2016-07-07 08:14:23',113,'EXECUTED','3:d331ce5f04aca9071c5b897396d81098','Create Index','',NULL,'2.0.5'),('2','upul','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10043,'MARK_RAN','3:b1811e5e43321192b275d6e2fe2fa564','Add Foreign Key Constraint','Create the foreign key from the privilege required for to edit\n			a person attribute type and the privilege.privilege column',NULL,'2.0.5'),('200805281223','bmckown','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10045,'MARK_RAN','3:b1fc37f9ec96eac9203f0808c2f4ac26','Create Table, Add Foreign Key Constraint','Create the concept_complex table',NULL,'2.0.5'),('200805281224','bmckown','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10046,'MARK_RAN','3:ea32453830c2215bdb209770396002e7','Add Column','Adding the value_complex column to obs.  This may take a long time if you have a large number of observations.',NULL,'2.0.5'),('200805281225','bmckown','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10047,'MARK_RAN','3:5281031bcc075df3b959e94da4adcaa9','Insert Row','Adding a \'complex\' Concept Datatype',NULL,'2.0.5'),('200805281226','bmckown','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10048,'MARK_RAN','3:9a49a3d002485f3a77134d98fb7c8cd8','Drop Table (x2)','Dropping the mimetype and complex_obs tables as they aren\'t needed in the new complex obs setup',NULL,'2.0.5'),('200809191226','smbugua','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10049,'MARK_RAN','3:eed0aa27b44ecf668c81e457d99fa7de','Add Column','Adding the hl7 archive message_state column so that archives can be tracked\n			(preCondition database_version check in place because this change was in the old format in trunk for a while)',NULL,'2.0.5'),('200809191927','smbugua','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10050,'MARK_RAN','3:f0e4fab64749e42770e62e9330c2d288','Rename Column, Modify Column','Adding the hl7 archive message_state column so that archives can be tracked',NULL,'2.0.5'),('200811261102','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10044,'EXECUTED','3:158dd028359ebfd4f1c9bf2e76a5e143','Update Data','Fix field property for new Tribe person attribute',NULL,'2.0.5'),('200901101524','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10051,'EXECUTED','3:feb4a087d13657164e5c3bc787b7f83f','Modify Column','Changing datatype of drug.retire_reason from DATETIME to varchar(255)',NULL,'2.0.5'),('200901130950','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10052,'EXECUTED','3:f1e5e7124bdb4f7378866fdb691e2780','Delete Data (x2)','Remove Manage Tribes and View Tribes privileges from all roles',NULL,'2.0.5'),('200901130951','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10053,'EXECUTED','3:54ac8683819837cc04f1a16b6311d668','Delete Data (x2)','Remove Manage Mime Types, View Mime Types, and Purge Mime Types privilege',NULL,'2.0.5'),('200901161126','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10054,'EXECUTED','3:871b9364dd87b6bfcc0005f40b6eb399','Delete Data','Removed the database_version global property',NULL,'2.0.5'),('20090121-0949','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10055,'EXECUTED','3:8639e35e0238019af2f9e326dd5cbc22','Custom SQL','Switched the default xslt to use PV1-19 instead of PV1-1',NULL,'2.0.5'),('20090122-0853','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:31',10056,'EXECUTED','3:4903c6f81f0309313013851f09a26b85','Custom SQL, Add Lookup Table, Drop Foreign Key Constraint, Delete Data (x2), Drop Table','Remove duplicate concept name tags',NULL,'2.0.5'),('20090123-0305','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10057,'MARK_RAN','3:48cdf2b28fcad687072ac8133e46cba6','Add Unique Constraint','Add unique constraint to the tags table',NULL,'2.0.5'),('20090214-2246','isherman','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10063,'EXECUTED','3:d16c607266238df425db61908e7c8745','Custom SQL','Add weight and cd4 to patientGraphConcepts user property (mysql specific)',NULL,'2.0.5'),('20090214-2247','isherman','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10064,'MARK_RAN','3:e4eeb4a09c2ab695bbde832cd7b6047d','Custom SQL','Add weight and cd4 to patientGraphConcepts user property (using standard sql)',NULL,'2.0.5'),('200902142212','ewolodzko','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10242,'MARK_RAN','3:df93fa2841295b29a0fcd4225c46d1a3','Add Column','Add a sortWeight field to PersonAttributeType',NULL,'2.0.5'),('200902142213','ewolodzko','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10243,'EXECUTED','3:288804e42d575fe62c852ed9daa9d59d','Custom SQL','Add default sortWeights to all current PersonAttributeTypes',NULL,'2.0.5'),('20090224-1002-create-visit_type','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10384,'MARK_RAN','3:ea3c0b323da2d51cf43e982177eace96','Create Table, Add Foreign Key Constraint (x3)','Create visit type table',NULL,'2.0.5'),('20090224-1229','Keelhaul+bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10058,'MARK_RAN','3:f8433194bcb29073c17c7765ce61aab2','Create Table, Add Foreign Key Constraint (x2)','Add location tags table',NULL,'2.0.5'),('20090224-1250','Keelhaul+bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10059,'MARK_RAN','3:8935a56fac2ad91275248d4675c2c090','Create Table, Add Foreign Key Constraint (x2), Add Primary Key','Add location tag map table',NULL,'2.0.5'),('20090224-1256','Keelhaul+bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10060,'MARK_RAN','3:9c0e7238dd1daad9edff381ba22a3ada','Add Column, Add Foreign Key Constraint','Add parent_location column to location table',NULL,'2.0.5'),('20090225-1551','dthomas','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10001,'MARK_RAN','3:a3aed1685bd1051a8c4fae0eab925954',NULL,NULL,NULL,NULL),('20090301-1259','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10062,'EXECUTED','3:21f2ac06dee26613b73003cd1f247ea8','Update Data (x2)','Fixes the description for name layout global property',NULL,'2.0.5'),('20090316-1008','vanand','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10000,'MARK_RAN','3:baa49982f1106c65ba33c845bba149b3',NULL,NULL,NULL,NULL),('20090316-1008-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10432,'EXECUTED','3:aeeb6c14cd22ffa121a2582e04025f5a','Modify Column (x36)','(Fixed)Changing from smallint to BOOLEAN type on BOOLEAN properties',NULL,'2.0.5'),('200903210905','mseaton','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10065,'MARK_RAN','3:720bb7a3f71f0c0a911d3364e55dd72f','Create Table, Add Foreign Key Constraint (x3)','Add a table to enable generic storage of serialized objects',NULL,'2.0.5'),('200903210905-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10066,'EXECUTED','3:a11519f50deeece1f9760d3fc1ac3f05','Modify Column','(Fixed)Add a table to enable generic storage of serialized objects',NULL,'2.0.5'),('20090402-1515-38-cohort','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10071,'MARK_RAN','3:5c65821ef168d9e8296466be5990ae08','Add Column','Adding \"uuid\" column to cohort table',NULL,'2.0.5'),('20090402-1515-38-concept','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10072,'MARK_RAN','3:8004d09d6e2a34623b8d0a13d6c38dc4','Add Column','Adding \"uuid\" column to concept table',NULL,'2.0.5'),('20090402-1515-38-concept_answer','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10073,'MARK_RAN','3:adf3f4ebf7e0eb55eb6927dea7ce2a49','Add Column','Adding \"uuid\" column to concept_answer table',NULL,'2.0.5'),('20090402-1515-38-concept_class','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10074,'MARK_RAN','3:f39e190a2e12c7a6163a0d8a82544228','Add Column','Adding \"uuid\" column to concept_class table',NULL,'2.0.5'),('20090402-1515-38-concept_datatype','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10075,'MARK_RAN','3:d68b3f2323626fee7b433f873a019412','Add Column','Adding \"uuid\" column to concept_datatype table',NULL,'2.0.5'),('20090402-1515-38-concept_description','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10076,'MARK_RAN','3:7d043672ede851c5dcd717171f953c75','Add Column','Adding \"uuid\" column to concept_description table',NULL,'2.0.5'),('20090402-1515-38-concept_map','bwolfe','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10002,'MARK_RAN','3:c1884f56bd70a205b86e7c4038e6c6f9',NULL,NULL,NULL,NULL),('20090402-1515-38-concept_name','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10077,'MARK_RAN','3:822888c5ba1132f6783fbd032c21f238','Add Column','Adding \"uuid\" column to concept_name table',NULL,'2.0.5'),('20090402-1515-38-concept_name_tag','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10078,'MARK_RAN','3:dcb584d414ffd8133c97e42585bd34cd','Add Column','Adding \"uuid\" column to concept_name_tag table',NULL,'2.0.5'),('20090402-1515-38-concept_proposal','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10079,'MARK_RAN','3:fe19ecccb704331741c227aa72597789','Add Column','Adding \"uuid\" column to concept_proposal table',NULL,'2.0.5'),('20090402-1515-38-concept_set','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10080,'MARK_RAN','3:cdc72e16eaec2244c09e9e2fedf5806b','Add Column','Adding \"uuid\" column to concept_set table',NULL,'2.0.5'),('20090402-1515-38-concept_source','bwolfe','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10003,'MARK_RAN','3:ad101415b93eaf653871eddd4fe4fc17',NULL,NULL,NULL,NULL),('20090402-1515-38-concept_state_conversion','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10081,'MARK_RAN','3:5ce8a6cdbfa8742b033b0b1c12e4cd42','Add Column','Adding \"uuid\" column to concept_state_conversion table',NULL,'2.0.5'),('20090402-1515-38-drug','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10082,'MARK_RAN','3:6869bd44f51cb7f63f758fbd8a7fe156','Add Column','Adding \"uuid\" column to drug table',NULL,'2.0.5'),('20090402-1515-38-encounter','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10083,'MARK_RAN','3:0808491f7ec59827a0415f2949b9d90e','Add Column','Adding \"uuid\" column to encounter table',NULL,'2.0.5'),('20090402-1515-38-encounter_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10084,'MARK_RAN','3:9aaac835f4d9579386990d4990ffb9d6','Add Column','Adding \"uuid\" column to encounter_type table',NULL,'2.0.5'),('20090402-1515-38-field','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10085,'MARK_RAN','3:dfee5fe509457ef12b14254bab9e6df5','Add Column','Adding \"uuid\" column to field table',NULL,'2.0.5'),('20090402-1515-38-field_answer','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10086,'MARK_RAN','3:c378494d6e9ae45b278c726256619cd7','Add Column','Adding \"uuid\" column to field_answer table',NULL,'2.0.5'),('20090402-1515-38-field_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10087,'MARK_RAN','3:dfb47f0b85d5bdad77f3a15cc4d180ec','Add Column','Adding \"uuid\" column to field_type table',NULL,'2.0.5'),('20090402-1515-38-form','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10088,'MARK_RAN','3:eb707ff99ed8ca2945a43175b904dea4','Add Column','Adding \"uuid\" column to form table',NULL,'2.0.5'),('20090402-1515-38-form_field','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10089,'MARK_RAN','3:635701ccda0484966f45f0e617119100','Add Column','Adding \"uuid\" column to form_field table',NULL,'2.0.5'),('20090402-1515-38-global_property','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10090,'MARK_RAN','3:1c62ba666b60eaa88ee3a90853f3bf59','Add Column','Adding \"uuid\" column to global_property table',NULL,'2.0.5'),('20090402-1515-38-hl7_in_archive','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10091,'MARK_RAN','3:9c5015280eff821924416112922fd94d','Add Column','Adding \"uuid\" column to hl7_in_archive table',NULL,'2.0.5'),('20090402-1515-38-hl7_in_error','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10092,'MARK_RAN','3:35b94fc079e6de9ada4329a7bbc55645','Add Column','Adding \"uuid\" column to hl7_in_error table',NULL,'2.0.5'),('20090402-1515-38-hl7_in_queue','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10093,'MARK_RAN','3:494d9eaaed055d0c5af4b4d85db2095d','Add Column','Adding \"uuid\" column to hl7_in_queue table',NULL,'2.0.5'),('20090402-1515-38-hl7_source','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10094,'MARK_RAN','3:8bc9839788ef5ab415ccf020eb04a1f7','Add Column','Adding \"uuid\" column to hl7_source table',NULL,'2.0.5'),('20090402-1515-38-location','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10095,'MARK_RAN','3:7e6b762f813310c72026677d540dee57','Add Column','Adding \"uuid\" column to location table',NULL,'2.0.5'),('20090402-1515-38-location_tag','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10096,'MARK_RAN','3:6a94a67e776662268d42f09cf7c66ac0','Add Column','Adding \"uuid\" column to location_tag table',NULL,'2.0.5'),('20090402-1515-38-note','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10097,'MARK_RAN','3:f0fd7b6750d07c973aad667b170cdfa8','Add Column','Adding \"uuid\" column to note table',NULL,'2.0.5'),('20090402-1515-38-notification_alert','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10098,'MARK_RAN','3:f2865558fb76c7584f6e86786b0ffdea','Add Column','Adding \"uuid\" column to notification_alert table',NULL,'2.0.5'),('20090402-1515-38-notification_template','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10099,'MARK_RAN','3:c05536d99eb2479211cb10010d48a2e9','Add Column','Adding \"uuid\" column to notification_template table',NULL,'2.0.5'),('20090402-1515-38-obs','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10100,'MARK_RAN','3:ba99d7eccba2185e9d5ebab98007e577','Add Column','Adding \"uuid\" column to obs table',NULL,'2.0.5'),('20090402-1515-38-orders','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10102,'MARK_RAN','3:732a2d4fd91690d544f0c63bdb65819f','Add Column','Adding \"uuid\" column to orders table',NULL,'2.0.5'),('20090402-1515-38-order_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10101,'MARK_RAN','3:137552884c5eb5af4c3f77c90df514cb','Add Column','Adding \"uuid\" column to order_type table',NULL,'2.0.5'),('20090402-1515-38-patient_identifier','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10103,'MARK_RAN','3:1a9ddcd8997bcf1a9668051d397e41c1','Add Column','Adding \"uuid\" column to patient_identifier table',NULL,'2.0.5'),('20090402-1515-38-patient_identifier_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10104,'MARK_RAN','3:6170d6caa73320fd2433fba0a16e8029','Add Column','Adding \"uuid\" column to patient_identifier_type table',NULL,'2.0.5'),('20090402-1515-38-patient_program','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10105,'MARK_RAN','3:8fb284b435669717f4b5aaa66e61fc10','Add Column','Adding \"uuid\" column to patient_program table',NULL,'2.0.5'),('20090402-1515-38-patient_state','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10106,'MARK_RAN','3:b67eb1bbd3e2912a646f56425c38631f','Add Column','Adding \"uuid\" column to patient_state table',NULL,'2.0.5'),('20090402-1515-38-person','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10107,'MARK_RAN','3:2b89eb77976b9159717e9d7b83c34cf1','Add Column','Adding \"uuid\" column to person table',NULL,'2.0.5'),('20090402-1515-38-person_address','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10108,'MARK_RAN','3:cfdb17b16b6d15477bc72d4d19ac3f29','Add Column','Adding \"uuid\" column to person_address table',NULL,'2.0.5'),('20090402-1515-38-person_attribute','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10109,'MARK_RAN','3:2f6b7fa688987b32d99cda348c6f6c46','Add Column','Adding \"uuid\" column to person_attribute table',NULL,'2.0.5'),('20090402-1515-38-person_attribute_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10110,'MARK_RAN','3:38d4dce320f2fc35db9dfcc2eafc093e','Add Column','Adding \"uuid\" column to person_attribute_type table',NULL,'2.0.5'),('20090402-1515-38-person_name','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10111,'MARK_RAN','3:339f02d6797870f9e7dd704f093b088c','Add Column','Adding \"uuid\" column to person_name table',NULL,'2.0.5'),('20090402-1515-38-privilege','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10112,'MARK_RAN','3:41f52c4340fdc9f0825ea9660edea8ec','Add Column','Adding \"uuid\" column to privilege table',NULL,'2.0.5'),('20090402-1515-38-program','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10113,'MARK_RAN','3:a72f80159cdbd576906cd3b9069d425b','Add Column','Adding \"uuid\" column to program table',NULL,'2.0.5'),('20090402-1515-38-program_workflow','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10114,'MARK_RAN','3:c69183f7e1614d5a338c0d0944f1e754','Add Column','Adding \"uuid\" column to program_workflow table',NULL,'2.0.5'),('20090402-1515-38-program_workflow_state','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10115,'MARK_RAN','3:e25b0fa351bb667af3ff562855f66bb6','Add Column','Adding \"uuid\" column to program_workflow_state table',NULL,'2.0.5'),('20090402-1515-38-relationship','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10116,'MARK_RAN','3:95407167e9f4984de1d710a83371ebd1','Add Column','Adding \"uuid\" column to relationship table',NULL,'2.0.5'),('20090402-1515-38-relationship_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10117,'MARK_RAN','3:f8755b127c004d11a43bfd6558be01b7','Add Column','Adding \"uuid\" column to relationship_type table',NULL,'2.0.5'),('20090402-1515-38-report_object','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10118,'MARK_RAN','3:b7ce0784e817be464370a3154fd4aa9c','Add Column','Adding \"uuid\" column to report_object table',NULL,'2.0.5'),('20090402-1515-38-report_schema_xml','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10119,'MARK_RAN','3:ce7ae79a3e3ce429a56fa658c48889b5','Add Column','Adding \"uuid\" column to report_schema_xml table',NULL,'2.0.5'),('20090402-1515-38-role','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10120,'MARK_RAN','3:f33887a0b51ab366d414e16202cf55db','Add Column','Adding \"uuid\" column to role table',NULL,'2.0.5'),('20090402-1515-38-serialized_object','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10121,'MARK_RAN','3:341cfbdff8ebf188d526bf3348619dcc','Add Column','Adding \"uuid\" column to serialized_object table',NULL,'2.0.5'),('20090402-1516-cohort','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10122,'EXECUTED','3:110084035197514c8d640b915230cf72','Update Data','Generating UUIDs for all rows in cohort table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10123,'EXECUTED','3:a44bc743cb837d88f7371282f3a5871e','Update Data','Generating UUIDs for all rows in concept table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_answer','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10124,'EXECUTED','3:f01d7278b153fa10a7d741607501ae1e','Update Data','Generating UUIDs for all rows in concept_answer table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_class','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10125,'EXECUTED','3:786f0ec8beec453ea9487f2e77f9fb4d','Update Data','Generating UUIDs for all rows in concept_class table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_datatype','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10126,'EXECUTED','3:b828e9851365ec70531dabd250374989','Update Data','Generating UUIDs for all rows in concept_datatype table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_description','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10127,'EXECUTED','3:37dbfc43c73553c9c9ecf11206714cc4','Update Data','Generating UUIDs for all rows in concept_description table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_map','bwolfe','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10004,'MARK_RAN','3:e843f99c0371aabee21ca94fcef01f39',NULL,NULL,NULL,NULL),('20090402-1516-concept_name','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10128,'EXECUTED','3:dd414ae9367287c9c03342a79abd1d62','Update Data','Generating UUIDs for all rows in concept_name table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_name_tag','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10129,'EXECUTED','3:cd7b5d0ceeb90b2254708b44c10d03e8','Update Data','Generating UUIDs for all rows in concept_name_tag table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_proposal','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10130,'EXECUTED','3:fb1cfa9c5decbafc3293f3dd1d87ff2b','Update Data','Generating UUIDs for all rows in concept_proposal table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_set','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10131,'EXECUTED','3:3b7f3851624014e740f89bc9a431feaa','Update Data','Generating UUIDs for all rows in concept_set table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-concept_source','bwolfe','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10005,'MARK_RAN','3:53da91ae3e39d7fb7ebca91df3bfd9a6',NULL,NULL,NULL,NULL),('20090402-1516-concept_state_conversion','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10132,'EXECUTED','3:23197d24e498ad86d4e001b183cc0c6b','Update Data','Generating UUIDs for all rows in concept_state_conversion table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-drug','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10133,'EXECUTED','3:40b47df80bd425337b7bdd8b41497967','Update Data','Generating UUIDs for all rows in drug table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-encounter','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10134,'EXECUTED','3:40146708b71d86d4c8c5340767a98f5e','Update Data','Generating UUIDs for all rows in encounter table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-encounter_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10135,'EXECUTED','3:738c6b6244a84fc8e6d582bcd472ffe6','Update Data','Generating UUIDs for all rows in encounter_type table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-field','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10136,'EXECUTED','3:98d2a1550e867e4ef303a4cc47ed904d','Update Data','Generating UUIDs for all rows in field table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-field_answer','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10137,'EXECUTED','3:82bdfe361286d261724eef97dd89e358','Update Data','Generating UUIDs for all rows in field_answer table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-field_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10138,'EXECUTED','3:19a8d007f6147651240ebb9539d3303a','Update Data','Generating UUIDs for all rows in field_type table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-form','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10139,'EXECUTED','3:026ddf1c9050c7367d4eb57dd4105322','Update Data','Generating UUIDs for all rows in form table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-form_field','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10140,'EXECUTED','3:a8b0bcdb35830c2badfdcb9b1cfdd3b5','Update Data','Generating UUIDs for all rows in form_field table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-global_property','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10141,'EXECUTED','3:75a5b4a9473bc9c6bfbabf8e77b0cda7','Update Data','Generating UUIDs for all rows in global_property table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-hl7_in_archive','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10142,'EXECUTED','3:09891436d8ea0ad14f7b52fd05daa237','Update Data','Generating UUIDs for all rows in hl7_in_archive table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-hl7_in_error','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10143,'EXECUTED','3:8d276bbd8bf9d9d1c64756f37ef91ed3','Update Data','Generating UUIDs for all rows in hl7_in_error table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-hl7_in_queue','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10144,'EXECUTED','3:25e8f998171accd46860717f93690ccc','Update Data','Generating UUIDs for all rows in hl7_in_queue table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-hl7_source','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10145,'EXECUTED','3:45c06e034d7158a0d09afae60c4c83d6','Update Data','Generating UUIDs for all rows in hl7_source table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-location','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10146,'EXECUTED','3:fce0f7eaab989f2ff9664fc66d6b8419','Update Data','Generating UUIDs for all rows in location table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-location_tag','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10147,'EXECUTED','3:50f26d1376ea108bbb65fd4d0633e741','Update Data','Generating UUIDs for all rows in location_tag table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-note','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10148,'EXECUTED','3:f5a0eea2a7c59fffafa674de4356e621','Update Data','Generating UUIDs for all rows in note table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-notification_alert','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10149,'EXECUTED','3:481fbab9bd53449903ac193894adbc28','Update Data','Generating UUIDs for all rows in notification_alert table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-notification_template','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10150,'EXECUTED','3:a4a2990465c4c99747f83ea880cac46a','Update Data','Generating UUIDs for all rows in notification_template table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-obs','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10151,'EXECUTED','3:26d80fdd889922821244f84e3f8039e7','Update Data','Generating UUIDs for all rows in obs table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-orders','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10153,'EXECUTED','3:ec3bc80540d78f416e1d4eef62e8e15a','Update Data','Generating UUIDs for all rows in orders table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-order_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10152,'EXECUTED','3:cae66b98b889c7ee1c8d6ab270a8d0d5','Update Data','Generating UUIDs for all rows in order_type table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-patient_identifier','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10154,'EXECUTED','3:647906cc7cf1fde9b7644b8f2541664f','Update Data','Generating UUIDs for all rows in patient_identifier table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-patient_identifier_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10155,'EXECUTED','3:85f8db0310c15a74b17e968c7730ae12','Update Data','Generating UUIDs for all rows in patient_identifier_type table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-patient_program','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10156,'EXECUTED','3:576b7db39f0212f8e92b6f4e1844ea30','Update Data','Generating UUIDs for all rows in patient_program table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-patient_state','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10157,'EXECUTED','3:250eab0f97fc4eeb4f1a930fbccfcf08','Update Data','Generating UUIDs for all rows in patient_state table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-person','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10158,'EXECUTED','3:cedc8bcd77ade51558fb2d12916e31a4','Update Data','Generating UUIDs for all rows in person table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-person_address','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10159,'EXECUTED','3:0f817424ca41e5c5b459591d6e18b3c6','Update Data','Generating UUIDs for all rows in person_address table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-person_attribute','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10160,'EXECUTED','3:7f9e09b1267c4a787a9d3e37acfd5746','Update Data','Generating UUIDs for all rows in person_attribute table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-person_attribute_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10161,'EXECUTED','3:1e5f84054b7b7fdf59673e2260f48d9d','Update Data','Generating UUIDs for all rows in person_attribute_type table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-person_name','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10162,'EXECUTED','3:f827da2c097b01ca9073c258b19e9540','Update Data','Generating UUIDs for all rows in person_name table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-privilege','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10163,'EXECUTED','3:2ab150a53c91ded0c5b53fa99fde4ba2','Update Data','Generating UUIDs for all rows in privilege table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-program','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10164,'EXECUTED','3:132b63f2efcf781187602e043122e7ff','Update Data','Generating UUIDs for all rows in program table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-program_workflow','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10165,'EXECUTED','3:d945359ed4bb6cc6a21f4554a0c50a33','Update Data','Generating UUIDs for all rows in program_workflow table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-program_workflow_state','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10166,'EXECUTED','3:4bc093882ac096562d63562ac76a1ffa','Update Data','Generating UUIDs for all rows in program_workflow_state table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-relationship','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10167,'EXECUTED','3:25e22c04ada4808cc31fd48f23703333','Update Data','Generating UUIDs for all rows in relationship table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-relationship_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10168,'EXECUTED','3:562ad77e9453595c9cd22a2cdde3cc41','Update Data','Generating UUIDs for all rows in relationship_type table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-report_object','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10169,'EXECUTED','3:8531f740c64a0d1605225536c1be0860','Update Data','Generating UUIDs for all rows in report_object table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-report_schema_xml','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10170,'EXECUTED','3:cd9efe4d62f2754b057d2d409d6e826a','Update Data','Generating UUIDs for all rows in report_schema_xml table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-role','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10171,'EXECUTED','3:f75bfc36ad13cb9324b9520804a60141','Update Data','Generating UUIDs for all rows in role table via built in uuid function.',NULL,'2.0.5'),('20090402-1516-serialized_object','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10172,'EXECUTED','3:c809b71d2444a8a8e2c5e5574d344c82','Update Data','Generating UUIDs for all rows in serialized_object table via built in uuid function.',NULL,'2.0.5'),('20090402-1517','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:35',10181,'MARK_RAN','3:4edd135921eb263d4811cf1c22ef4846','Custom Change','Adding UUIDs to all rows in all columns via a java class. (This will take a long time on large databases)',NULL,'2.0.5'),('20090402-1518','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:35',10182,'MARK_RAN','3:a9564fc8de85d37f4748a3fa1e69281c','Add Not-Null Constraint (x52)','Now that UUID generation is done, set the uuid columns to not \"NOT NULL\"',NULL,'2.0.5'),('20090402-1519-cohort','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:35',10183,'EXECUTED','3:260c435f1cf3e3f01d953d630c7a578b','Create Index','Creating unique index on cohort.uuid column',NULL,'2.0.5'),('20090402-1519-concept','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:35',10184,'EXECUTED','3:9e363ee4b39e7fdfb547e3a51ad187c7','Create Index','Creating unique index on concept.uuid column',NULL,'2.0.5'),('20090402-1519-concept_answer','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:35',10185,'EXECUTED','3:34b049a3fd545928760968beb1e98e00','Create Index','Creating unique index on concept_answer.uuid column',NULL,'2.0.5'),('20090402-1519-concept_class','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:36',10186,'EXECUTED','3:0fc95dccef2343850adb1fe49d60f3c3','Create Index','Creating unique index on concept_class.uuid column',NULL,'2.0.5'),('20090402-1519-concept_datatype','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:36',10187,'EXECUTED','3:0cf065b0f780dc2eeca994628af49a34','Create Index','Creating unique index on concept_datatype.uuid column',NULL,'2.0.5'),('20090402-1519-concept_description','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:36',10188,'EXECUTED','3:16ce0ad6c3e37071bbfcaad744693d0f','Create Index','Creating unique index on concept_description.uuid column',NULL,'2.0.5'),('20090402-1519-concept_map','bwolfe','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10006,'MARK_RAN','3:b8a320c1d44ab94e785c9ae6c41378f3',NULL,NULL,NULL,NULL),('20090402-1519-concept_name','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:36',10189,'EXECUTED','3:0d5866c0d3eadc8df09b1a7c160508ca','Create Index','Creating unique index on concept_name.uuid column',NULL,'2.0.5'),('20090402-1519-concept_name_tag','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:37',10190,'EXECUTED','3:7ba597ec0fb5fbfba615ac97df642072','Create Index','Creating unique index on concept_name_tag.uuid column',NULL,'2.0.5'),('20090402-1519-concept_proposal','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:37',10191,'EXECUTED','3:79f9f4af9669c2b03511832a23db55e0','Create Index','Creating unique index on concept_proposal.uuid column',NULL,'2.0.5'),('20090402-1519-concept_set','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:37',10192,'EXECUTED','3:f5ba4e2d5ddd4ec66f43501b9749cf70','Create Index','Creating unique index on concept_set.uuid column',NULL,'2.0.5'),('20090402-1519-concept_source','bwolfe','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10007,'MARK_RAN','3:c7c47d9c2876bfa53542885e304b21e7',NULL,NULL,NULL,NULL),('20090402-1519-concept_state_conversion','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:37',10193,'EXECUTED','3:cc9d9bb0d5eb9f6583cd538919b42b9a','Create Index','Creating unique index on concept_state_conversion.uuid column',NULL,'2.0.5'),('20090402-1519-drug','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:38',10194,'EXECUTED','3:8cac800e9f857e29698e1c80ab7e6a52','Create Index','Creating unique index on drug.uuid column',NULL,'2.0.5'),('20090402-1519-encounter','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:38',10195,'EXECUTED','3:8fd623411a44ffb0d4e3a4139e916585','Create Index','Creating unique index on encounter.uuid column',NULL,'2.0.5'),('20090402-1519-encounter_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:38',10196,'EXECUTED','3:71e0e1df8c290d8b6e81e281154661e0','Create Index','Creating unique index on encounter_type.uuid column',NULL,'2.0.5'),('20090402-1519-field','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:38',10197,'EXECUTED','3:36d9eba3e0a90061c6bf1c8aa483110e','Create Index','Creating unique index on field.uuid column',NULL,'2.0.5'),('20090402-1519-field_answer','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:39',10198,'EXECUTED','3:81572b572f758cac173b5d14516f600e','Create Index','Creating unique index on field_answer.uuid column',NULL,'2.0.5'),('20090402-1519-field_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:39',10199,'EXECUTED','3:a0c3927dfde900959131aeb1490a5f51','Create Index','Creating unique index on field_type.uuid column',NULL,'2.0.5'),('20090402-1519-form','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:39',10200,'EXECUTED','3:61147c780ce563776a1caed795661aca','Create Index','Creating unique index on form.uuid column',NULL,'2.0.5'),('20090402-1519-form_field','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:39',10201,'EXECUTED','3:bd9def4522865d181e42809f9dd5c116','Create Index','Creating unique index on form_field.uuid column',NULL,'2.0.5'),('20090402-1519-global_property','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:40',10202,'EXECUTED','3:0e6b84ad5fffa3fd49242b5475e8eb66','Create Index','Creating unique index on global_property.uuid column',NULL,'2.0.5'),('20090402-1519-hl7_in_archive','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:40',10203,'EXECUTED','3:d2f8921c170e416560c234aa74964346','Create Index','Creating unique index on hl7_in_archive.uuid column',NULL,'2.0.5'),('20090402-1519-hl7_in_error','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:40',10204,'EXECUTED','3:9ccec0729ea1b4eaa5068726f9045c25','Create Index','Creating unique index on hl7_in_error.uuid column',NULL,'2.0.5'),('20090402-1519-hl7_in_queue','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:40',10205,'EXECUTED','3:af537cb4134c3f2ed0357f3280ceb6fe','Create Index','Creating unique index on hl7_in_queue.uuid column',NULL,'2.0.5'),('20090402-1519-hl7_source','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:41',10206,'EXECUTED','3:a6d1847b6a590319206f65be9d1d3c9e','Create Index','Creating unique index on hl7_source.uuid column',NULL,'2.0.5'),('20090402-1519-location','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:41',10207,'EXECUTED','3:c435bd4b405d4f11d919777718aa055c','Create Index','Creating unique index on location.uuid column',NULL,'2.0.5'),('20090402-1519-location_tag','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:41',10208,'EXECUTED','3:33a8a54cde59b23a9cdb7740a9995e1a','Create Index','Creating unique index on location_tag.uuid column',NULL,'2.0.5'),('20090402-1519-note','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:41',10209,'EXECUTED','3:97279b2ce285e56613a10a77c5af32b2','Create Index','Creating unique index on note.uuid column',NULL,'2.0.5'),('20090402-1519-notification_alert','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:42',10210,'EXECUTED','3:a763255eddf8607f7d86afbb3099d4b5','Create Index','Creating unique index on notification_alert.uuid column',NULL,'2.0.5'),('20090402-1519-notification_template','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:42',10211,'EXECUTED','3:9a69bbb343077bc62acdf6a66498029a','Create Index','Creating unique index on notification_template.uuid column',NULL,'2.0.5'),('20090402-1519-obs','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:42',10212,'EXECUTED','3:de9a7a24e527542e6b4a73e2cd31a7f9','Create Index','Creating unique index on obs.uuid column',NULL,'2.0.5'),('20090402-1519-orders','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:42',10214,'EXECUTED','3:848c0a00a32c5eb25041ad058fd38263','Create Index','Creating unique index on orders.uuid column',NULL,'2.0.5'),('20090402-1519-order_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:42',10213,'EXECUTED','3:d938d263e0acf974d43ad81d2fbe05b0','Create Index','Creating unique index on order_type.uuid column',NULL,'2.0.5'),('20090402-1519-patient_identifier','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:43',10215,'EXECUTED','3:43389efa06408c8312d130654309d140','Create Index','Creating unique index on patient_identifier.uuid column',NULL,'2.0.5'),('20090402-1519-patient_identifier_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:43',10216,'EXECUTED','3:3ffe4f31a1c48d2545e8eed4127cc490','Create Index','Creating unique index on patient_identifier_type.uuid column',NULL,'2.0.5'),('20090402-1519-patient_program','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:43',10217,'EXECUTED','3:ce69defda5ba254914f2319f3a7aac02','Create Index','Creating unique index on patient_program.uuid column',NULL,'2.0.5'),('20090402-1519-patient_state','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:43',10218,'EXECUTED','3:a4ca15f62b3c8c43f7f47ef8b9e39cd3','Create Index','Creating unique index on patient_state.uuid column',NULL,'2.0.5'),('20090402-1519-person','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:44',10219,'EXECUTED','3:345a5d4e8dea4d56c1a0784e7b35a801','Create Index','Creating unique index on person.uuid column',NULL,'2.0.5'),('20090402-1519-person_address','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:44',10220,'EXECUTED','3:105ece744a45b624ea8990f152bb8300','Create Index','Creating unique index on person_address.uuid column',NULL,'2.0.5'),('20090402-1519-person_attribute','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:44',10221,'EXECUTED','3:67a8cdda8605c28f76314873d2606457','Create Index','Creating unique index on person_attribute.uuid column',NULL,'2.0.5'),('20090402-1519-person_attribute_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:44',10222,'EXECUTED','3:a234ad0ea13f32fc4529cf556151d611','Create Index','Creating unique index on person_attribute_type.uuid column',NULL,'2.0.5'),('20090402-1519-person_name','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:45',10223,'EXECUTED','3:d18e326ce221b4b1232ce2e355731338','Create Index','Creating unique index on person_name.uuid column',NULL,'2.0.5'),('20090402-1519-privilege','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:45',10224,'EXECUTED','3:47e7f70f34a213d870e2aeed795d5e3d','Create Index','Creating unique index on privilege.uuid column',NULL,'2.0.5'),('20090402-1519-program','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:45',10225,'EXECUTED','3:62f9d9ecd2325d5908237a769e9a8bc7','Create Index','Creating unique index on program.uuid column',NULL,'2.0.5'),('20090402-1519-program_workflow','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:45',10226,'EXECUTED','3:fabb3152f6055dc0071a2e5d6f573d2f','Create Index','Creating unique index on program_workflow.uuid column',NULL,'2.0.5'),('20090402-1519-program_workflow_state','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:46',10227,'EXECUTED','3:4fdf0c20aedcdc87b2c6058a1cc8fce7','Create Index','Creating unique index on program_workflow_state.uuid column',NULL,'2.0.5'),('20090402-1519-relationship','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:46',10228,'EXECUTED','3:c90617ca900b1aef3f29e71f693e8a25','Create Index','Creating unique index on relationship.uuid column',NULL,'2.0.5'),('20090402-1519-relationship_type','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:46',10229,'EXECUTED','3:c9f05aca70b6dad54af121b593587a29','Create Index','Creating unique index on relationship_type.uuid column',NULL,'2.0.5'),('20090402-1519-report_object','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:46',10230,'EXECUTED','3:6069b78580fd0d276f5dae9f3bdf21be','Create Index','Creating unique index on report_object.uuid column',NULL,'2.0.5'),('20090402-1519-report_schema_xml','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:47',10231,'EXECUTED','3:91499d332dda0577fd02b6a6b7b35e99','Create Index','Creating unique index on report_schema_xml.uuid column',NULL,'2.0.5'),('20090402-1519-role','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:47',10232,'EXECUTED','3:c535a800ceb006311bbb7a27e8bab6ea','Create Index','Creating unique index on role.uuid column',NULL,'2.0.5'),('20090402-1519-serialized_object','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:47',10233,'EXECUTED','3:e8f2b1c3a7a67aadc8499ebcb522c91a','Create Index','Creating unique index on serialized_object.uuid column',NULL,'2.0.5'),('20090408-1298','Cory McCarthy','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10068,'EXECUTED','3:defbd13a058ba3563e232c2093cd2b37','Modify Column','Changed the datatype for encounter_type to \'text\' instead of just 50 chars',NULL,'2.0.5'),('200904091023','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10067,'EXECUTED','3:48adc23e9c5d820a87f6c8d61dfb6b55','Delete Data (x4)','Remove Manage Tribes and View Tribes privileges from the privilege table and role_privilege table.\n			The privileges will be recreated by the Tribe module if it is installed.',NULL,'2.0.5'),('20090414-0804','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10173,'EXECUTED','3:479b4df8e3c746b5b96eeea422799774','Drop Foreign Key Constraint','Dropping foreign key on concept_set.concept_id table',NULL,'2.0.5'),('20090414-0805','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10174,'MARK_RAN','3:5017417439ff841eb036ceb94f3c5800','Drop Primary Key','Dropping primary key on concept set table',NULL,'2.0.5'),('20090414-0806','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:33',10175,'MARK_RAN','3:6b9cec59fd607569228bf87d4dffa1a5','Add Column','Adding new integer primary key to concept set table',NULL,'2.0.5'),('20090414-0807','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:34',10176,'MARK_RAN','3:57834f6c953f34035237e06a2dbed9c7','Create Index, Add Foreign Key Constraint','Adding index and foreign key to concept_set.concept_id column',NULL,'2.0.5'),('20090414-0808a','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:34',10177,'EXECUTED','3:6c9d9e6b85c1bf04fdbf9fdec316f2ea','Drop Foreign Key Constraint','Dropping foreign key on patient_identifier.patient_id column',NULL,'2.0.5'),('20090414-0808b','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:34',10178,'MARK_RAN','3:12e01363841135ed0dae46d71e7694cf','Drop Primary Key','Dropping non-integer primary key on patient identifier table before adding a new integer primary key',NULL,'2.0.5'),('20090414-0809','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:34',10179,'MARK_RAN','3:864765efa4cae1c8ffb1138d63f77017','Add Column','Adding new integer primary key to patient identifier table',NULL,'2.0.5'),('20090414-0810','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:35',10180,'MARK_RAN','3:4ca46ee358567e35c897a73c065e3367','Create Index, Add Foreign Key Constraint','Adding index and foreign key on patient_identifier.patient_id column',NULL,'2.0.5'),('20090414-0811a','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:47',10234,'EXECUTED','3:f027a0ad38c0f6302def391da78aaaee','Drop Foreign Key Constraint','Dropping foreign key on concept_word.concept_id column',NULL,'2.0.5'),('20090414-0811b','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:47',10236,'MARK_RAN','3:982d502e56854922542286cead4c09ce','Drop Primary Key','Dropping non-integer primary key on concept word table before adding new integer one',NULL,'2.0.5'),('20090414-0812','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:47',10237,'MARK_RAN','3:948e635fe3f63122856ca9b8a174352b','Add Column','Adding integer primary key to concept word table',NULL,'2.0.5'),('20090414-0812b','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10238,'MARK_RAN','3:bd7731e58f3db9b944905597a08eb6cb','Add Foreign Key Constraint','Re-adding foreign key for concept_word.concept_name_id',NULL,'2.0.5'),('200904271042','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10241,'MARK_RAN','3:db63ce704aff4741c52181d1c825ab62','Drop Column','Remove the now unused synonym column',NULL,'2.0.5'),('20090428-0811aa','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:47',10235,'MARK_RAN','3:58d8f3df1fe704714a7b4957a6c0e7f7','Drop Foreign Key Constraint','Removing concept_word.concept_name_id foreign key so that primary key can be changed to concept_word_id',NULL,'2.0.5'),('20090428-0854','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10239,'EXECUTED','3:11086a37155507c0238c9532f66b172b','Add Foreign Key Constraint','Adding foreign key for concept_word.concept_id column',NULL,'2.0.5'),('200905071626','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10070,'MARK_RAN','3:d29884c3ef8fd867c3c2ffbd557c14c2','Create Index','Add an index to the concept_word.concept_id column (This update may fail if it already exists)',NULL,'2.0.5'),('200905150814','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:32',10069,'EXECUTED','3:44c729b393232d702553e0768cf94994','Delete Data','Deleting invalid concept words',NULL,'2.0.5'),('200905150821','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10240,'EXECUTED','3:c0b7abc7eb00f243325b4a3fb2afc614','Custom SQL','Deleting duplicate concept word keys',NULL,'2.0.5'),('200906301606','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10244,'EXECUTED','3:de40c56c128997509d1d943ed047c5d2','Modify Column','Change person_attribute_type.sort_weight from an integer to a float',NULL,'2.0.5'),('200907161638-1','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10245,'EXECUTED','3:dfd352bdc4c5e6c88cd040d03c782e31','Modify Column','Change obs.value_numeric from a double(22,0) to a double',NULL,'2.0.5'),('200907161638-2','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10246,'EXECUTED','3:a8dc0bd1593e6c99a02db443bc4cb001','Modify Column','Change concept_numeric columns from a double(22,0) type to a double',NULL,'2.0.5'),('200907161638-3','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10247,'EXECUTED','3:47b8adbcd480660765dd117020a1e085','Modify Column','Change concept_set.sort_weight from a double(22,0) to a double',NULL,'2.0.5'),('200907161638-4','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10248,'EXECUTED','3:3ffccaa291298fea317eb7025c058492','Modify Column','Change concept_set_derived.sort_weight from a double(22,0) to a double',NULL,'2.0.5'),('200907161638-5','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10249,'EXECUTED','3:3b31cf625830c7e37fa638dbf9625000','Modify Column','Change drug table columns from a double(22,0) to a double',NULL,'2.0.5'),('200907161638-6','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10250,'EXECUTED','3:dc733faec1539038854c0b559b45da0e','Modify Column','Change drug_order.dose from a double(22,0) to a double',NULL,'2.0.5'),('200908291938-1','dthomas','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10008,'MARK_RAN','3:b99a6d7349d367c30e8b404979e07b89',NULL,NULL,NULL,NULL),('200908291938-2a','dthomas','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10009,'MARK_RAN','3:7e9e8d9bffcb6e602b155827f72a3856',NULL,NULL,NULL,NULL),('20090831-1039-38-scheduler_task_config','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10254,'MARK_RAN','3:54e254379235d5c8b569a00ac7dc9c3f','Add Column','Adding \"uuid\" column to scheduler_task_config table',NULL,'2.0.5'),('20090831-1040-scheduler_task_config','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10255,'EXECUTED','3:a9b26bdab35405050c052a9a3f763db0','Update Data','Generating UUIDs for all rows in scheduler_task_config table via built in uuid function.',NULL,'2.0.5'),('20090831-1041-scheduler_task_config','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10256,'MARK_RAN','3:25127273b2d501664ce325922b0c7db2','Custom Change','Adding UUIDs to all rows in scheduler_task_config table via a java class for non mysql/oracle/mssql databases.',NULL,'2.0.5'),('20090831-1042-scheduler_task_config','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10257,'EXECUTED','3:76d8a8b5d342fc4111034861537315cf','Add Not-Null Constraint','Now that UUID generation is done for scheduler_task_config, set the uuid column to not \"NOT NULL\"',NULL,'2.0.5'),('20090831-1043-scheduler_task_config','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10258,'EXECUTED','3:5408ed04284c4f5d57f5160ca5393733','Create Index','Creating unique index on scheduler_task_config.uuid column',NULL,'2.0.5'),('20090907-1','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10259,'MARK_RAN','3:d6f3ed289cdbce6229b1414ec626a33c','Rename Column','Rename the concept_source.date_voided column to date_retired',NULL,'2.0.5'),('20090907-2a','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10260,'MARK_RAN','3:b71e307e4e782cc5a851f764aa7fc0d0','Drop Foreign Key Constraint','Remove the concept_source.voided_by foreign key constraint',NULL,'2.0.5'),('20090907-2b','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10261,'MARK_RAN','3:14e07ebc0a1138ee973bbb26b568d16e','Rename Column, Add Foreign Key Constraint','Rename the concept_source.voided_by column to retired_by',NULL,'2.0.5'),('20090907-3','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10262,'MARK_RAN','3:adee9ced82158f9a9f3d64245ad591c6','Rename Column','Rename the concept_source.voided column to retired',NULL,'2.0.5'),('20090907-4','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10263,'MARK_RAN','3:ad9b6ed4ef3ae43556d3e8c9e2ec0f5c','Rename Column','Rename the concept_source.void_reason column to retire_reason',NULL,'2.0.5'),('20091001-1023','rcrichton','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10291,'MARK_RAN','3:2bf99392005da4e95178bd1e2c28a87b','Add Column','add retired column to relationship_type table',NULL,'2.0.5'),('20091001-1024','rcrichton','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10292,'MARK_RAN','3:31b7b10f75047606406cea156bcc255f','Add Column','add retired_by column to relationship_type table',NULL,'2.0.5'),('20091001-1025','rcrichton','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10293,'MARK_RAN','3:c6dd75893e5573baa0c7426ecccaa92d','Add Foreign Key Constraint','Create the foreign key from the relationship.retired_by to users.user_id.',NULL,'2.0.5'),('20091001-1026','rcrichton','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10294,'MARK_RAN','3:47cfbab54a8049948784a165ffe830af','Add Column','add date_retired column to relationship_type table',NULL,'2.0.5'),('20091001-1027','rcrichton','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10295,'MARK_RAN','3:2db32da70ac1e319909d692110b8654b','Add Column','add retire_reason column to relationship_type table',NULL,'2.0.5'),('200910271049-1','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10264,'EXECUTED','3:2e54d97b9f1b9f35b77cee691c23b7a9','Update Data (x5)','Setting core field types to have standard UUIDs',NULL,'2.0.5'),('200910271049-10','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10273,'EXECUTED','3:827070940f217296c11ce332dc8858ff','Update Data (x4)','Setting core roles to have standard UUIDs',NULL,'2.0.5'),('200910271049-2','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10265,'EXECUTED','3:3132d4cbfaab0c0b612c3fe1c55bd0f1','Update Data (x7)','Setting core person attribute types to have standard UUIDs',NULL,'2.0.5'),('200910271049-3','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10266,'EXECUTED','3:f4d1a9004f91b6885a86419bc02f9d0b','Update Data (x4)','Setting core encounter types to have standard UUIDs',NULL,'2.0.5'),('200910271049-4','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10267,'EXECUTED','3:0d4f7503bf8f00cb73338bb34305333a','Update Data (x12)','Setting core concept datatypes to have standard UUIDs',NULL,'2.0.5'),('200910271049-5','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10268,'EXECUTED','3:98d8ac75977e1b099a4e45d96c6b1d1a','Update Data (x4)','Setting core relationship types to have standard UUIDs',NULL,'2.0.5'),('200910271049-6','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10269,'EXECUTED','3:19355a03794869edad3889ac0adbdedf','Update Data (x15)','Setting core concept classes to have standard UUIDs',NULL,'2.0.5'),('200910271049-7','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10270,'EXECUTED','3:fe4c89654d02d74de6d8e4b265a33288','Update Data (x2)','Setting core patient identifier types to have standard UUIDs',NULL,'2.0.5'),('200910271049-8','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10271,'EXECUTED','3:dc4462b5b4b13c2bc306506848127556','Update Data','Setting core location to have standard UUIDs',NULL,'2.0.5'),('200910271049-9','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10272,'EXECUTED','3:de2a0ed2adafb53f025039e9e8c6719e','Update Data','Setting core hl7 source to have standard UUIDs',NULL,'2.0.5'),('200912031842','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10277,'EXECUTED','3:b966745213bedaeeabab8a874084bb95','Drop Foreign Key Constraint, Add Foreign Key Constraint','Changing encounter.provider_id to reference person instead of users',NULL,'2.0.5'),('200912031846-1','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10279,'MARK_RAN','3:23e728a7f214127cb91efd40ebbcc2d1','Add Column, Update Data','Adding person_id column to users table (if needed)',NULL,'2.0.5'),('200912031846-2','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10280,'MARK_RAN','3:8d57907defa7e92e018038d57cfa78b4','Update Data, Add Not-Null Constraint','Populating users.person_id',NULL,'2.0.5'),('200912031846-3','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10281,'EXECUTED','3:48a50742f2904682caa1bc469f5b87e3','Add Foreign Key Constraint, Set Column as Auto-Increment','Restoring foreign key constraint on users.person_id',NULL,'2.0.5'),('200912071501-1','arthurs','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10274,'EXECUTED','3:d1158b8a42127d7b8a4d5ad64cc7c225','Update Data','Change name for patient.searchMaxResults global property to person.searchMaxResults',NULL,'2.0.5'),('200912091819','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10282,'MARK_RAN','3:8c0b2b02a94b9c6c9529e1b29207464b','Add Column, Add Foreign Key Constraint','Adding retired metadata columns to users table',NULL,'2.0.5'),('200912091819-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10283,'EXECUTED','3:fd5fd1d2e6884662824bb78c8348fadf','Modify Column','(Fixed)users.retired to BOOLEAN',NULL,'2.0.5'),('200912091820','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10284,'MARK_RAN','3:cba73499d1c4d09b0e4ae3b55ecc7d84','Update Data','Migrating voided metadata to retired metadata for users table',NULL,'2.0.5'),('200912091821','djazayeri','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10012,'MARK_RAN','3:9b38d31ebfe427d1f8d6e8530687f29c',NULL,NULL,NULL,NULL),('200912140038','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10285,'MARK_RAN','3:be3aaa8da16b8a8841509faaeff070b4','Add Column','Adding \"uuid\" column to users table',NULL,'2.0.5'),('200912140039','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10286,'EXECUTED','3:5b2a81ac1efba5495962bfb86e51546d','Update Data','Generating UUIDs for all rows in users table via built in uuid function.',NULL,'2.0.5'),('200912140040','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10287,'MARK_RAN','3:c422b96e5b88eeae4f343d4f988cc4b2','Custom Change','Adding UUIDs to users table via a java class. (This will take a long time on large databases)',NULL,'2.0.5'),('200912141000-drug-add-date-changed','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10469,'MARK_RAN','3:9c9a75e3a78104e72de078ac217b0972','Add Column','Add date_changed column to drug table',NULL,'2.0.5'),('200912141001-drug-add-changed-by','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10470,'MARK_RAN','3:196629c722f52df68b5040e5266ac20f','Add Column, Add Foreign Key Constraint','Add changed_by column to drug table',NULL,'2.0.5'),('200912141552','madanmohan','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10275,'MARK_RAN','3:835b6b98a7a437d959255ac666c12759','Add Column, Add Foreign Key Constraint','Add changed_by column to encounter table',NULL,'2.0.5'),('200912141553','madanmohan','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10276,'MARK_RAN','3:7f768aa879beac091501ac9bb47ece4d','Add Column','Add date_changed column to encounter table',NULL,'2.0.5'),('20091215-0208','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10296,'EXECUTED','3:1c818a60d8ebc36f4b7911051c1f6764','Custom SQL','Prune concepts rows orphaned in concept_numeric tables',NULL,'2.0.5'),('20091215-0209','jmiranda','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10297,'EXECUTED','3:adeadc55e4dd484b1d63cf123e299371','Custom SQL','Prune concepts rows orphaned in concept_complex tables',NULL,'2.0.5'),('20091215-0210','jmiranda','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10011,'MARK_RAN','3:08e8550629e4d5938494500f61d10961',NULL,NULL,NULL,NULL),('200912151032','n.nehete','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10289,'EXECUTED','3:d7d8fededde8a27384ca1eb3f87f7914','Add Not-Null Constraint','Encounter Type should not be null when saving an Encounter',NULL,'2.0.5'),('200912211118','nribeka','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10010,'MARK_RAN','3:1f976b4eedf537d887451246d49db043',NULL,NULL,NULL,NULL),('201001072007','upul','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10290,'MARK_RAN','3:d5d60060fae8e9c30843b16b23bed9db','Add Column','Add last execution time column to scheduler_task_config table',NULL,'2.0.5'),('20100111-0111-associating-daemon-user-with-person','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10462,'MARK_RAN','3:bebb5c508bb53e7d5be6fb3aa259bd2f','Custom SQL','Associating daemon user with a person',NULL,'2.0.5'),('20100128-1','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10251,'MARK_RAN','3:eaa1b8e62aa32654480e7a476dc14a4a','Insert Row','Adding \'System Developer\' role again (see ticket #1499)',NULL,'2.0.5'),('20100128-2','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10252,'MARK_RAN','3:3c486c2ea731dfad7905518cac8d6e70','Update Data','Switching users back from \'Administrator\' to \'System Developer\' (see ticket #1499)',NULL,'2.0.5'),('20100128-3','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:48',10253,'MARK_RAN','3:9acf8cae5d210f88006191e79b76532c','Delete Data','Deleting \'Administrator\' role (see ticket #1499)',NULL,'2.0.5'),('20100306-095513a','thilini.hg','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10298,'MARK_RAN','3:b7a60c3c33a05a71dde5a26f35d85851','Drop Foreign Key Constraint','Dropping unused foreign key from notification alert table',NULL,'2.0.5'),('20100306-095513b','thilini.hg','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10299,'MARK_RAN','3:8a6ebb6aefe04b470d5b3878485f9cc3','Drop Column','Dropping unused user_id column from notification alert table',NULL,'2.0.5'),('20100322-0908','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10300,'MARK_RAN','3:94a8aae1d463754d7125cd546b4c590c','Add Column, Update Data','Adding sort_weight column to concept_answers table and initially sets the sort_weight to the concept_answer_id',NULL,'2.0.5'),('20100323-192043','ricardosbarbosa','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10317,'EXECUTED','3:c294c84ac7ff884d1e618f4eb74b0c52','Update Data, Delete Data (x2)','Removing the duplicate privilege \'Add Concept Proposal\' in favor of \'Add Concept Proposals\'',NULL,'2.0.5'),('20100330-190413','ricardosbarbosa','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10318,'EXECUTED','3:d706294defdfb73af9b44db7d37069d0','Update Data, Delete Data (x2)','Removing the duplicate privilege \'Edit Concept Proposal\' in favor of \'Edit Concept Proposals\'',NULL,'2.0.5'),('20100412-2217','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10301,'MARK_RAN','3:0c3a3ea15adefa620ab62145f412d0b6','Add Column','Adding \"uuid\" column to notification_alert_recipient table',NULL,'2.0.5'),('20100412-2218','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10302,'EXECUTED','3:6fae383b5548c214d2ad2c76346e32e3','Update Data','Generating UUIDs for all rows in notification_alert_recipient table via built in uuid function.',NULL,'2.0.5'),('20100412-2219','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10303,'MARK_RAN','3:1401fe5f2d0c6bc23afa70b162e15346','Custom Change','Adding UUIDs to notification_alert_recipient table via a java class (if needed).',NULL,'2.0.5'),('20100412-2220','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10304,'EXECUTED','3:bf4474dd5700b570e158ddc8250c470b','Add Not-Null Constraint','Now that UUID generation is done, set the notification_alert_recipient.uuid column to not \"NOT NULL\"',NULL,'2.0.5'),('20100413-1509','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10305,'MARK_RAN','3:7a3ee61077e4dee1ceb4fe127afc835f','Rename Column','Change location_tag.tag to location_tag.name',NULL,'2.0.5'),('20100415-forgotten-from-before','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10288,'EXECUTED','3:d17699fbec80bd035ecb348ae5382754','Add Not-Null Constraint','Adding not null constraint to users.uuid',NULL,'2.0.5'),('20100419-1209','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10385,'MARK_RAN','3:f87b773f9a8e05892fdbe8740042abb5','Create Table, Add Foreign Key Constraint (x7), Create Index','Create the visit table and add the foreign key for visit_type',NULL,'2.0.5'),('20100419-1209-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10386,'EXECUTED','3:cb5970216f918522df3a059e29506c27','Modify Column','(Fixed)Changed visit.voided to BOOLEAN',NULL,'2.0.5'),('20100423-1402','slorenz','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10307,'MARK_RAN','3:3534020f1c68f70b0e9851d47a4874d6','Create Index','Add an index to the encounter.encounter_datetime column to speed up statistical\n			analysis.',NULL,'2.0.5'),('20100423-1406','slorenz','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10308,'MARK_RAN','3:f058162398862f0bdebc12d7eb54551b','Create Index','Add an index to the obs.obs_datetime column to speed up statistical analysis.',NULL,'2.0.5'),('20100426-1111-add-not-null-personid-contraint','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10463,'EXECUTED','3:a0b90b98be85aabbdebd957744ab805a','Add Not-Null Constraint','Add the not null person id contraint',NULL,'2.0.5'),('20100426-1111-remove-not-null-personid-contraint','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10309,'EXECUTED','3:5bc2abe108ab2765e36294ff465c63a0','Drop Not-Null Constraint','Drop the not null person id contraint',NULL,'2.0.5'),('20100426-1947','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10310,'MARK_RAN','3:09adbdc9cb72dee82e67080b01d6578e','Insert Row','Adding daemon user to users table',NULL,'2.0.5'),('20100512-1400','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10312,'MARK_RAN','3:0fbfb53e2e194543d7b3eaa59834e1e6','Insert Row','Create core order_type for drug orders',NULL,'2.0.5'),('20100513-1947','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10311,'EXECUTED','3:068c2bd55d9c731941fe9ef66f0011fb','Delete Data (x2)','Removing scheduler.username and scheduler.password global properties',NULL,'2.0.5'),('20100517-1545','wyclif and djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10313,'EXECUTED','3:39a68e6b1954a0954d0f8d0c660a7aff','Custom Change','Switch boolean concepts/observations to be stored as coded',NULL,'2.0.5'),('20100525-818-1','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10319,'MARK_RAN','3:ed9dcb5bd0d7312db3123825f9bb4347','Create Table, Add Foreign Key Constraint (x2)','Create active list type table.',NULL,'2.0.5'),('20100525-818-1-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10320,'EXECUTED','3:4a648a54797fef2222764a7ee0b5e05a','Modify Column','(Fixed)Change active_list_type.retired to BOOLEAN',NULL,'2.0.5'),('20100525-818-2','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10321,'MARK_RAN','3:bc5a86f0245f6f822a0d343b2fcf8dc6','Create Table, Add Foreign Key Constraint (x7)','Create active list table',NULL,'2.0.5'),('20100525-818-2-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10322,'EXECUTED','3:0a2879b368319f6d1e16d0d4417f4492','Modify Column','(Fixed)Change active_list_type.retired to BOOLEAN',NULL,'2.0.5'),('20100525-818-3','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10323,'MARK_RAN','3:d382e7b9e23cdcc33ccde2d3f0473c41','Create Table, Add Foreign Key Constraint','Create allergen table',NULL,'2.0.5'),('20100525-818-4','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10324,'MARK_RAN','3:1d6f1abd297c8da5a49d4885d0d34dfb','Create Table','Create problem table',NULL,'2.0.5'),('20100525-818-5','syhaas','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10325,'MARK_RAN','3:2ac51b2e8813d61428367bad9fadaa33','Insert Row (x2)','Inserting default active list types',NULL,'2.0.5'),('20100526-1025','Harsha.cse','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10314,'EXECUTED','3:66ec6553564d30fd63df7c2de41c674f','Drop Not-Null Constraint (x2)','Drop Not-Null constraint from location column in Encounter and Obs table',NULL,'2.0.5'),('20100603-1625-1-person_address','sapna','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10366,'MARK_RAN','3:6048aa2c393c1349de55a5003199fb81','Add Column','Adding \"date_changed\" column to person_address table',NULL,'2.0.5'),('20100603-1625-2-person_address','sapna','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10367,'MARK_RAN','3:5194e3b45b70b003e33d7ab0495f3015','Add Column, Add Foreign Key Constraint','Adding \"changed_by\" column to person_address table',NULL,'2.0.5'),('20100604-0933a','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10315,'EXECUTED','3:9b51b236846a8940de581e199cd76cb2','Add Default Value','Changing the default value to 2 for \'message_state\' column in \'hl7_in_archive\' table',NULL,'2.0.5'),('20100604-0933b','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10316,'EXECUTED','3:67fc4c12418b500aaf3723e8845429e3','Update Data','Converting 0 and 1 to 2 for \'message_state\' column in \'hl7_in_archive\' table',NULL,'2.0.5'),('20100607-1550a','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10326,'MARK_RAN','3:bfb6250277efd8c81326fe8c3dbdfe35','Add Column','Adding \'concept_name_type\' column to concept_name table',NULL,'2.0.5'),('20100607-1550b','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10327,'MARK_RAN','3:3d43124d8265fbf05f1ef4839f14bece','Add Column','Adding \'locale_preferred\' column to concept_name table',NULL,'2.0.5'),('20100607-1550b-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10328,'EXECUTED','3:d0dc8dfe3ac629aecee81ccc11dec9c2','Modify Column','(Fixed)Change concept_name.locale_preferred to BOOLEAN',NULL,'2.0.5'),('20100607-1550c','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10329,'EXECUTED','3:b6573617d37609ae7195fd7a495e2776','Drop Foreign Key Constraint','Dropping foreign key constraint on concept_name_tag_map.concept_name_tag_id',NULL,'2.0.5'),('20100607-1550d','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10330,'EXECUTED','3:f30fd17874ac8294389ee2a44ca7d6ab','Update Data, Delete Data (x2)','Setting the concept name type for short names',NULL,'2.0.5'),('20100607-1550f','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10331,'EXECUTED','3:74026cd4543ebbf561999a81c276224d','Update Data, Delete Data (x2)','Converting concept names with \'preferred\' and \'preferred_XX\' concept name tags to preferred names',NULL,'2.0.5'),('20100607-1550g','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10334,'EXECUTED','3:c3c0a17e0a21d36f38bb2af8f0939da7','Delete Data (x2)','Deleting \'default\' and \'synonym\' concept name tags',NULL,'2.0.5'),('20100607-1550h','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10335,'EXECUTED','3:be7b967ed0e7006373bb616b63726144','Custom Change','Validating and attempting to fix invalid concepts and ConceptNames',NULL,'2.0.5'),('20100607-1550i','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10336,'EXECUTED','3:b6260c13bf055f7917c155596502a24b','Add Foreign Key Constraint','Restoring foreign key constraint on concept_name_tag_map.concept_name_tag_id',NULL,'2.0.5'),('20100621-1443','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10337,'EXECUTED','3:16b4bc3512029cf8d3b3c6bee86ed712','Modify Column','Modify the error_details column of hl7_in_error to hold\n			stacktraces',NULL,'2.0.5'),('201008021047','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10338,'MARK_RAN','3:8612ede2553aab53950fa43d2f8def32','Create Index','Add an index to the person_name.family_name2 to speed up patient and person searches',NULL,'2.0.5'),('201008201345','mseaton','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10339,'EXECUTED','3:5fbbb6215e66847c86483ee7177c3682','Custom Change','Validates Program Workflow States for possible configuration problems and reports warnings',NULL,'2.0.5'),('201008242121','misha680','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10340,'EXECUTED','3:2319aed08c4f6dcd43d4ace5cdf94650','Modify Column','Make person_name.person_id not NULLable',NULL,'2.0.5'),('20100924-1110','mseaton','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10341,'MARK_RAN','3:05ea5f3b806ba47f4a749d3a348c59f7','Add Column, Add Foreign Key Constraint','Add location_id column to patient_program table',NULL,'2.0.5'),('201009281047','misha680','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10342,'MARK_RAN','3:02b5b9a183729968cd4189798ca034bd','Drop Column','Remove the now unused default_charge column',NULL,'2.0.5'),('201010051745','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10343,'EXECUTED','3:04ba6f526a71fc0a2b016fd77eaf9ff5','Update Data','Setting the global property \'patient.identifierRegex\' to an empty string',NULL,'2.0.5'),('201010051746','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10344,'EXECUTED','3:cb12dfc563d82529de170ffedf948f90','Update Data','Setting the global property \'patient.identifierSuffix\' to an empty string',NULL,'2.0.5'),('201010151054','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10345,'MARK_RAN','3:26c8ae0c53225f82d4c2a85c09ad9785','Create Index','Adding index to form.published column',NULL,'2.0.5'),('201010151055','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10346,'MARK_RAN','3:1efabdfd082ff2b0a34f570831f74ce5','Create Index','Adding index to form.retired column',NULL,'2.0.5'),('201010151056','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10347,'MARK_RAN','3:00273104184bb4d2bb7155befc77efc3','Create Index','Adding multi column index on form.published and form.retired columns',NULL,'2.0.5'),('201010261143','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10348,'MARK_RAN','3:c02de7e2726893f80ecd1f3ae778cba5','Rename Column','Rename neighborhood_cell column to address3 and increase the size to 255 characters',NULL,'2.0.5'),('201010261145','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10349,'MARK_RAN','3:2d053c2e9b604403df8a408a6bb4f3f8','Rename Column','Rename township_division column to address4 and increase the size to 255 characters',NULL,'2.0.5'),('201010261147','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10350,'MARK_RAN','3:592eee2241fdb1039ba08be07b54a422','Rename Column','Rename subregion column to address5 and increase the size to 255 characters',NULL,'2.0.5'),('201010261149','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10351,'MARK_RAN','3:059e5bf4092d930304f9f0fc305939d9','Rename Column','Rename region column to address6 and increase the size to 255 characters',NULL,'2.0.5'),('201010261151','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10352,'MARK_RAN','3:8756b20f505f8981a43ece7233ce3e2f','Rename Column','Rename neighborhood_cell column to address3 and increase the size to 255 characters',NULL,'2.0.5'),('201010261153','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10353,'MARK_RAN','3:9805b9a214fca5a3509a82864274678e','Rename Column','Rename township_division column to address4 and increase the size to 255 characters',NULL,'2.0.5'),('201010261156','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10354,'MARK_RAN','3:894f4e47fbdc74be94e6ebc9d6fce91e','Rename Column','Rename subregion column to address5 and increase the size to 255 characters',NULL,'2.0.5'),('201010261159','crecabarren','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10355,'MARK_RAN','3:b1827790c63813e6a73d83e2b2d36504','Rename Column','Rename region column to address6 and increase the size to 255 characters',NULL,'2.0.5'),('20101029-1016','gobi/prasann','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10368,'MARK_RAN','3:714ad65f5d84bdcd4d944a4d5583e4d3','Create Table, Add Unique Constraint','Create table to store concept stop words to avoid in search key indexing',NULL,'2.0.5'),('20101029-1026','gobi/prasann','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10369,'MARK_RAN','3:83534d43a9a9cc1ea3a80f1d5f5570af','Insert Row (x10)','Inserting the initial concept stop words',NULL,'2.0.5'),('201011011600','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10357,'MARK_RAN','3:29b35d66dc4168e03e1844296e309327','Create Index','Adding index to message_state column in HL7 archive table',NULL,'2.0.5'),('201011011605','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10358,'EXECUTED','3:c604bc0967765f50145f76e80a4bbc99','Custom Change','Moving \"deleted\" HL7s from HL7 archive table to queue table',NULL,'2.0.5'),('201011051300','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10365,'MARK_RAN','3:fea4ad8ce44911eeaab8ac8c1cc9122d','Create Index','Adding index on notification_alert.date_to_expire column',NULL,'2.0.5'),('201012081716','nribeka','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10363,'MARK_RAN','3:4a97a93f2632fc0c3b088b24535ee481','Delete Data','Removing concept that are concept derived and the datatype',NULL,'2.0.5'),('201012081717','nribeka','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10364,'MARK_RAN','3:ad3d0a18bda7e4869d264c70b8cd8d1d','Drop Table','Removing concept derived tables',NULL,'2.0.5'),('20101209-10000-encounter-add-visit-id-column','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10387,'MARK_RAN','3:7045a94731ef25e04724c77fc97494b4','Add Column, Add Foreign Key Constraint','Adding visit_id column to encounter table',NULL,'2.0.5'),('20101209-1721','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10359,'MARK_RAN','3:351460e0f822555b77acff1a89bec267','Add Column','Add \'weight\' column to concept_word table',NULL,'2.0.5'),('20101209-1722','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10360,'MARK_RAN','3:d63107017bdcef0e28d7ad5e4df21ae5','Create Index','Adding index to concept_word.weight column',NULL,'2.0.5'),('20101209-1723','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10361,'MARK_RAN','3:25d45d7d5bbff4b24bcc8ff8d34d70d2','Insert Row','Insert a row into the schedule_task_config table for the ConceptIndexUpdateTask',NULL,'2.0.5'),('20101209-1731','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10362,'MARK_RAN','3:6de3e859f77856fe939d3ae6a73b4752','Update Data','Setting the value of \'start_on_startup\' to trigger off conceptIndexUpdateTask on startup',NULL,'2.0.5'),('201012092009','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:52',10356,'EXECUTED','3:15a029c4ffe65710a56d402e608d319a','Modify Column (x10)','Increasing length of address fields in person_address and location to 255',NULL,'2.0.5'),('2011-07-12-1947-add-outcomesConcept-to-program','grwarren','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10398,'MARK_RAN','3:ea2bb0a2ddeade662f956ef113d020ab','Add Column, Add Foreign Key Constraint','Adding the outcomesConcept property to Program',NULL,'2.0.5'),('2011-07-12-2005-add-outcome-to-patientprogram','grwarren','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10399,'MARK_RAN','3:57baf47f9b09b3df649742d69be32015','Add Column, Add Foreign Key Constraint','Adding the outcome property to PatientProgram',NULL,'2.0.5'),('201101121434','gbalaji,gobi','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10378,'MARK_RAN','3:96320c51e6e296e9dc65866a61268e45','Drop Column','Dropping unused date_started column from obs table',NULL,'2.0.5'),('201101221453','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10377,'EXECUTED','3:4088d4906026cc1430fa98e04d294b13','Modify Column','Increasing the serialized_data column of serialized_object to hold mediumtext',NULL,'2.0.5'),('20110124-1030','surangak','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10380,'MARK_RAN','3:e17eee5b8c4bb236a0ea6e6ade5abed7','Add Foreign Key Constraint','Adding correct foreign key for concept_answer.answer_drug',NULL,'2.0.5'),('20110125-1435','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10371,'MARK_RAN','3:dadd9da1dad5f2863f8f6bb24b29d598','Add Column','Adding \'start_date\' column to person_address table',NULL,'2.0.5'),('20110125-1436','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10372,'MARK_RAN','3:68cec89409d2419fe9439f4753a23036','Add Column','Adding \'end_date\' column to person_address table',NULL,'2.0.5'),('201101271456-add-enddate-to-relationship','misha680','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10389,'MARK_RAN','3:b593b864d4a870e3b7ba6b61fda57c8d','Add Column','Adding the end_date column to relationship.',NULL,'2.0.5'),('201101271456-add-startdate-to-relationship','misha680','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10388,'MARK_RAN','3:82020a9f33747f58274196619439781e','Add Column','Adding the start_date column to relationship.',NULL,'2.0.5'),('20110201-1625-1','arahulkmit','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10373,'MARK_RAN','3:4f1b23efba67de1917e312942fe7e744','Add Column','Adding \"date_changed\" column to patient_identifier table',NULL,'2.0.5'),('20110201-1625-2','arahulkmit','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10374,'MARK_RAN','3:01467a1db56ef3db87dc537d40ab22eb','Add Column, Add Foreign Key Constraint','Adding \"changed_by\" column to patient_identifier table',NULL,'2.0.5'),('20110201-1626-1','arahulkmit','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10375,'MARK_RAN','3:63397ce933d1c78309648425fba66a17','Add Column','Adding \"date_changed\" column to relationship table',NULL,'2.0.5'),('20110201-1626-2','arahulkmit','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10376,'MARK_RAN','3:21dae026e42d05b2ebc8fe51408c147f','Add Column, Add Foreign Key Constraint','Adding \"changed_by\" column to relationship table',NULL,'2.0.5'),('201102081800','gbalaji,gobi','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10379,'MARK_RAN','3:779ca58f39b4e3a14a313f8fc416c242','Drop Column','Dropping unused date_stopped column from obs table',NULL,'2.0.5'),('20110218-1206','rubailly','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10013,'MARK_RAN','3:8be61726cd3fed87215557efd284434f',NULL,NULL,NULL,NULL),('20110218-1210','rubailly','liquibase-update-to-latest.xml','2011-09-15 00:00:00',10013,'MARK_RAN','3:4f8818ba08f3a9ce2e2ededfdf5b6fcd',NULL,NULL,NULL,NULL),('201102280948','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:49',10278,'EXECUTED','3:98e1075808582c97377651d02faf8f46','Drop Foreign Key Constraint','Removing the foreign key from users.user_id to person.person_id if it still exists',NULL,'2.0.5'),('20110301-1030a','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10413,'MARK_RAN','3:5256e8010fb4c375e2a1ef502176cc2f','Rename Table','Renaming the concept_source table to concept_reference_source',NULL,'2.0.5'),('20110301-1030b','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10414,'MARK_RAN','3:6fc5f514cd9c2ee14481a7f0b10a0c7c','Create Table, Add Foreign Key Constraint (x4)','Adding concept_reference_term table',NULL,'2.0.5'),('20110301-1030b-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10415,'EXECUTED','3:3cf3ba141e6571b900e695b49b6c48a9','Modify Column','(Fixed)Change concept_reference_term.retired to BOOLEAN',NULL,'2.0.5'),('20110301-1030c','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10416,'MARK_RAN','3:d8407baf728a1db5ad5db7c138cb59cb','Create Table, Add Foreign Key Constraint (x3)','Adding concept_map_type table',NULL,'2.0.5'),('20110301-1030c-fix','sunbiz','liquibase-update-to-latest.xml','2011-09-19 00:00:00',10014,'MARK_RAN','3:c02f2825633f1a43fc9303ac21ba2c02',NULL,NULL,NULL,NULL),('20110301-1030d','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10417,'MARK_RAN','3:222ef47c65625a17c268a8f68edaa16e','Rename Table','Renaming the concept_map table to concept_reference_map',NULL,'2.0.5'),('20110301-1030e','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10418,'MARK_RAN','3:50be921cf53ce4a357afc0bac8928495','Add Column','Adding concept_reference_term_id column to concept_reference_map table',NULL,'2.0.5'),('20110301-1030f','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10419,'MARK_RAN','3:5faead5506cbcde69490fef985711d66','Custom Change','Inserting core concept map types',NULL,'2.0.5'),('20110301-1030g','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10420,'MARK_RAN','3:affc4d2a4e3143046cfb75b583c7399a','Add Column, Add Foreign Key Constraint','Adding concept_map_type_id column and a foreign key constraint to concept_reference_map table',NULL,'2.0.5'),('20110301-1030h','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10421,'MARK_RAN','3:4bf584dc7b25a180cc82edb56e1b0e5b','Add Column, Add Foreign Key Constraint','Adding changed_by column and a foreign key constraint to concept_reference_map table',NULL,'2.0.5'),('20110301-1030i','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10422,'MARK_RAN','3:f4d0468db79007d0355f6f461603b2f7','Add Column','Adding date_changed column and a foreign key constraint to concept_reference_map table',NULL,'2.0.5'),('20110301-1030j','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10423,'MARK_RAN','3:a7dc8b89e37fe36263072b43670d7f11','Create Table, Add Foreign Key Constraint (x5)','Adding concept_reference_term_map table',NULL,'2.0.5'),('20110301-1030m','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10424,'MARK_RAN','3:b286407bfcdf3853512cb15009c816f1','Custom Change','Creating concept reference terms from existing rows in the concept_reference_map table',NULL,'2.0.5'),('20110301-1030n','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10425,'MARK_RAN','3:01868c1383e5c9c409282b50e67e878c','Add Foreign Key Constraint','Adding foreign key constraint to concept_reference_map.concept_reference_term_id column',NULL,'2.0.5'),('20110301-1030o','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10426,'MARK_RAN','3:eea9343959864edea569d5a2a2358469','Drop Foreign Key Constraint','Dropping foreign key constraint on concept_reference_map.source column',NULL,'2.0.5'),('20110301-1030p','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10427,'MARK_RAN','3:01bf8c07a05f22df2286a4ee27a7acb4','Drop Column','Dropping concept_reference_map.source column',NULL,'2.0.5'),('20110301-1030q','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10428,'MARK_RAN','3:f45caaf1c7daa7f2cb036f46a20aa4b1','Drop Column','Dropping concept_reference_map.source_code column',NULL,'2.0.5'),('20110301-1030r','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10429,'MARK_RAN','3:23fd6bc96ee0a497cf330ed24ec0075b','Drop Column','Dropping concept_reference_map.comment column',NULL,'2.0.5'),('201103011751','abbas','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10381,'EXECUTED','3:4857dcbefa75784da912bca5caba21b5','Create Table, Add Foreign Key Constraint (x3)','Create the person_merge_log table',NULL,'2.0.5'),('20110326-1','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10456,'EXECUTED','3:3376a34edf88bf2868fd75ba2fb0f6c3','Add Column, Add Foreign Key Constraint','Add obs.previous_version column (TRUNK-420)',NULL,'2.0.5'),('20110326-2','Knoll_Frank','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10459,'EXECUTED','3:7c068bfe918b9d87fefa9f8508e92f58','Custom SQL','Fix all the old void_reason content and add in the new previous_version to the matching obs row (POTENTIALLY VERY SLOW FOR LARGE OBS TABLES)',NULL,'2.0.5'),('20110329-2317','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10382,'EXECUTED','3:371be45e2a3616ce17b6f50862ca196d','Delete Data','Removing \'View Encounters\' privilege from Anonymous user',NULL,'2.0.5'),('20110329-2318','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10383,'EXECUTED','3:eb2ece117d8508e843d11eeed7676b21','Delete Data','Removing \'View Observations\' privilege from Anonymous user',NULL,'2.0.5'),('20110425-1600-create-visit-attribute-type-table','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10390,'MARK_RAN','3:3cf419ea9657f9a072881cafb2543d77','Create Table, Add Foreign Key Constraint (x3)','Creating visit_attribute_type table',NULL,'2.0.5'),('20110425-1600-create-visit-attribute-type-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10391,'EXECUTED','3:e4b62b99750c9ee4c213a7bc3101f8a6','Modify Column','(Fixed)Change visit_attribute_type.retired to BOOLEAN',NULL,'2.0.5'),('20110425-1700-create-visit-attribute-table','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10393,'MARK_RAN','3:24e1e30a41f9f5d92f337444fb45402a','Create Table, Add Foreign Key Constraint (x5)','Creating visit_attribute table',NULL,'2.0.5'),('20110425-1700-create-visit-attribute-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10394,'EXECUTED','3:8ab9102da66058c326c0a5089de053e8','Modify Column','(Fixed)Change visit_attribute.voided to BOOLEAN',NULL,'2.0.5'),('20110426-11701','zabil','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10435,'MARK_RAN','3:56caae006a3af14242e2ea57627004c7','Create Table, Add Foreign Key Constraint (x4)','Create provider table',NULL,'2.0.5'),('20110426-11701-create-provider-table','dkayiwa','liquibase-schema-only.xml','2016-07-07 08:14:22',87,'EXECUTED','3:56caae006a3af14242e2ea57627004c7','Create Table, Add Foreign Key Constraint (x4)','Create provider table',NULL,'2.0.5'),('20110426-11701-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10436,'EXECUTED','3:f222ec7d41ce0255c667fd79b70bffd2','Modify Column','(Fixed)Change provider.retired to BOOLEAN',NULL,'2.0.5'),('20110510-11702-create-provider-attribute-type-table','zabil','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10437,'EXECUTED','3:7478ac84804d46a4f2b3daa63efe99be','Create Table, Add Foreign Key Constraint (x3)','Creating provider_attribute_type table',NULL,'2.0.5'),('20110510-11702-create-provider-attribute-type-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10438,'EXECUTED','3:479636c7572a649889527f670eaff533','Modify Column','(Fixed)Change provider_attribute_type.retired to BOOLEAN',NULL,'2.0.5'),('20110628-1400-create-provider-attribute-table','kishoreyekkanti','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10440,'EXECUTED','3:298aaacafd48547be294f4c9b7c40d35','Create Table, Add Foreign Key Constraint (x5)','Creating provider_attribute table',NULL,'2.0.5'),('20110628-1400-create-provider-attribute-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10441,'EXECUTED','3:14d85967e968d0bcd7a49ddeb6f3e540','Modify Column','(Fixed)Change provider_attribute.voided to BOOLEAN',NULL,'2.0.5'),('20110705-2300-create-encounter-role-table','kishoreyekkanti','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10442,'MARK_RAN','3:a381ef81f10e4f7443b4d4c8d6231de8','Create Table, Add Foreign Key Constraint (x3)','Creating encounter_role table',NULL,'2.0.5'),('20110705-2300-create-encounter-role-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10443,'EXECUTED','3:bed2af9d6c3d49eacbdaf2174e682671','Modify Column','(Fixed)Change encounter_role.retired to BOOLEAN',NULL,'2.0.5'),('20110705-2311-create-encounter-role-table','dkayiwa','liquibase-schema-only.xml','2016-07-07 08:14:23',88,'EXECUTED','3:a381ef81f10e4f7443b4d4c8d6231de8','Create Table, Add Foreign Key Constraint (x3)','Creating encounter_role table',NULL,'2.0.5'),('20110708-2105','cta','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10397,'MARK_RAN','3:a20e9bb27a1aca73a646ad81ef2b9deb','Add Unique Constraint','Add unique constraint to the concept_source table',NULL,'2.0.5'),('201107192313-change-length-of-regex-column','jtellez','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10395,'EXECUTED','3:db001544cc0f5a1ff42524a9292b028b','Modify Column','Increasing maximum length of patient identifier type regex format',NULL,'2.0.5'),('20110811-1205-create-encounter-provider-table','sree/vishnu','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10444,'EXECUTED','3:e20ca5412e37df98c58a39552aafb5ad','Create Table, Add Foreign Key Constraint (x3)','Creating encounter_provider table',NULL,'2.0.5'),('20110811-1205-create-encounter-provider-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10445,'EXECUTED','3:8decefa15168e68297f5f2782991c552','Modify Column','(Fixed)Change encounter_provider.voided to BOOLEAN',NULL,'2.0.5'),('20110817-1544-create-location-attribute-type-table','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10400,'MARK_RAN','3:41fa30c01ec2d1107beccb8126146464','Create Table, Add Foreign Key Constraint (x3)','Creating location_attribute_type table',NULL,'2.0.5'),('20110817-1544-create-location-attribute-type-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10401,'EXECUTED','3:53aff6217c6a9a8f1ca414703b1a8720','Modify Column','(Fixed)Change visit_attribute.retired to BOOLEAN',NULL,'2.0.5'),('20110817-1601-create-location-attribute-table','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10403,'MARK_RAN','3:c7cb1b35d68451d10badeb445df599b9','Create Table, Add Foreign Key Constraint (x5)','Creating location_attribute table',NULL,'2.0.5'),('20110817-1601-create-location-attribute-table-fix','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10404,'EXECUTED','3:2450e230f3eda291203485bca6904377','Modify Column','(Fixed)Change visit_attribute.retired to BOOLEAN',NULL,'2.0.5'),('20110819-1455-insert-unknown-encounter-role','raff','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10446,'EXECUTED','3:bfe0b994a3c0a62d0d4c8f7d941991c7','Insert Row','Inserting the unknown encounter role into the encounter_role table',NULL,'2.0.5'),('20110825-1000-creating-providers-for-persons-from-encounter','raff','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10447,'EXECUTED','3:04bc8aa9859f6f8dda065e272ba12e0d','Custom SQL','Creating providers for persons from the encounter table',NULL,'2.0.5'),('20110825-1000-drop-provider-id-column-from-encounter-table','raff','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10449,'EXECUTED','3:2137e4b5198aa5f12059ee0e8837fb04','Drop Foreign Key Constraint, Drop Column','Dropping the provider_id column from the encounter table',NULL,'2.0.5'),('20110825-1000-migrating-providers-to-encounter-provider','raff','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10448,'EXECUTED','3:e7c39080453e862d5a4013c48c9225fc','Custom SQL','Migrating providers from the encounter table to the encounter_provider table',NULL,'2.0.5'),('2011091-0749','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',125,'EXECUTED','3:3534020f1c68f70b0e9851d47a4874d6','Create Index','',NULL,'2.0.5'),('2011091-0750','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',126,'EXECUTED','3:f058162398862f0bdebc12d7eb54551b','Create Index','',NULL,'2.0.5'),('20110913-0300','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10430,'MARK_RAN','3:7ad8f362e4cc6df6e37135cc37546d0d','Drop Foreign Key Constraint, Add Foreign Key Constraint','Remove ON DELETE CASCADE from relationship table for person_a',NULL,'2.0.5'),('20110913-0300b','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10431,'MARK_RAN','3:2486028ce670bdea2a5ced509a335170','Drop Foreign Key Constraint, Add Foreign Key Constraint','Remove ON DELETE CASCADE from relationship table for person_b',NULL,'2.0.5'),('20110914-0104','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',317,'EXECUTED','3:b1811e5e43321192b275d6e2fe2fa564','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0114','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',69,'EXECUTED','3:dac2ff60a4f99315d68948e9582af011','Create Table','',NULL,'2.0.5'),('20110914-0117','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',318,'EXECUTED','3:5b7f746286a955da60c9fec8d663a0e3','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0245','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',319,'EXECUTED','3:48cdf2b28fcad687072ac8133e46cba6','Add Unique Constraint','',NULL,'2.0.5'),('20110914-0306','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',70,'EXECUTED','3:037f98fda886cde764171990d168e97d','Create Table','',NULL,'2.0.5'),('20110914-0308','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',320,'EXECUTED','3:6309ad633777b0faf1d9fa394699a789','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0310','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',321,'EXECUTED','3:8c53c44af44d75aadf6cedfc9d13ded1','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0312','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',71,'EXECUTED','3:2a39901427c9e7b84c8578ff7b3099bb','Create Table','',NULL,'2.0.5'),('20110914-0314','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',322,'EXECUTED','3:9cbe2e14482f88864f94d5e630a88b62','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0315','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',323,'EXECUTED','3:18cd917d56887ad924dad367470a8461','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0317','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',98,'EXECUTED','3:cffbf258ca090d095401957df4168175','Add Primary Key','',NULL,'2.0.5'),('20110914-0321','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',324,'EXECUTED','3:67723ac8a4583366b78c9edc413f89eb','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0434','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',326,'EXECUTED','3:081831e316a82683102f298a91116e92','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0435','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',327,'EXECUTED','3:03fa6c6a37a61480c95d5b75e30d4846','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0448','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',72,'EXECUTED','3:ffa1ef2b17d77f87dccbdea0c51249de','Create Table','',NULL,'2.0.5'),('20110914-0453','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',325,'EXECUTED','3:ea43c7690888a7fd47aa7ba39f8006e2','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0509','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',122,'EXECUTED','3:d29884c3ef8fd867c3c2ffbd557c14c2','Create Index','',NULL,'2.0.5'),('20110914-0943','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',123,'EXECUTED','3:c48f2441d83f121db30399d9cd5f7f8b','Create Index','',NULL,'2.0.5'),('20110914-0945','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',328,'EXECUTED','3:ea1fbb819a84a853b4a97f93bd5b8600','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110914-0956','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',124,'EXECUTED','3:719aa7e4120c11889d91214196acfd4c','Create Index','',NULL,'2.0.5'),('20110914-0958','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',329,'EXECUTED','3:ad98b3c7ae60001d0e0a7b927177fb72','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0258','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',330,'EXECUTED','3:bd7731e58f3db9b944905597a08eb6cb','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0259','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',331,'EXECUTED','3:11086a37155507c0238c9532f66b172b','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0357','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',332,'EXECUTED','3:05d531e66cbc42e1eb2d42c8bcf20bc8','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0547','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',333,'EXECUTED','3:f3b0fc223476060082626b3849ee20ad','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0552','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',334,'EXECUTED','3:46e5067fb13cefd224451b25abbd03ae','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0603','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',335,'EXECUTED','3:ca4f567e4d75ede0553e8b32012e4141','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0610','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',336,'EXECUTED','3:d6c6a22571e304640b2ff1be52c76977','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0634','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',337,'EXECUTED','3:c6dd75893e5573baa0c7426ecccaa92d','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0751','sunbiz','liquibase-core-data.xml','2016-07-07 08:14:29',10029,'EXECUTED','3:010949e257976520a6e8c87e419c9435','Insert Row','',NULL,'2.0.5'),('20110915-0803','sunbiz','liquibase-core-data.xml','2016-07-07 08:14:29',10036,'EXECUTED','3:4a09e1959df71d38fa77b249bf032edc','Insert Row','',NULL,'2.0.5'),('20110915-0823','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:26',338,'EXECUTED','3:beb831615b748a06a8b21dcaeba8c40d','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0824','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',339,'EXECUTED','3:90f1a69f5cae1d2b3b3a2fa8cb1bace2','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0825','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',74,'EXECUTED','3:17eab4b1c4c36b54d8cf8ca26083105c','Create Table','',NULL,'2.0.5'),('20110915-0836','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',340,'EXECUTED','3:53f76b5f2c20d5940518a1b14ebab33e','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0837','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',341,'EXECUTED','3:936ecde7ac26efdd1a4c29260183609c','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0838','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',342,'EXECUTED','3:fc1e68e753194b2f83e014daa0f7cb3e','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0839','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',343,'EXECUTED','3:90bfb3d0edfcfc8091a2ffd943a54e88','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0840','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',344,'EXECUTED','3:9af8eca0bc6b58c3816f871d9f6d5af8','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0841','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',345,'EXECUTED','3:2ca812616a13bac6b0463bf26b9a0fe3','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0842','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',346,'EXECUTED','3:4fd619ffdedac0cf141a7dd1b6e92f9b','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0845','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',75,'EXECUTED','3:4e799d7e5a15e823116caa01ab7ed808','Create Table','',NULL,'2.0.5'),('20110915-0846','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',347,'EXECUTED','3:a41f6272aa79f3259ba24f0a31c51e72','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-0847','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',76,'EXECUTED','3:8c1e49cd3d6402648ee7732ba9948785','Create Table','',NULL,'2.0.5'),('20110915-0848','sunbiz','liquibase-core-data.xml','2016-07-07 08:14:29',10037,'EXECUTED','3:cf7989886ae2624508fdf64b7b656727','Insert Row (x2)','',NULL,'2.0.5'),('20110915-0848','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',77,'EXECUTED','3:071de39e44036bd8adb2b24b011b7369','Create Table','',NULL,'2.0.5'),('20110915-0903','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',348,'EXECUTED','3:b6260c13bf055f7917c155596502a24b','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1045','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',127,'EXECUTED','3:8612ede2553aab53950fa43d2f8def32','Create Index','',NULL,'2.0.5'),('20110915-1049','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',349,'EXECUTED','3:b71f1caa3d14aa6282ef58e2a002f999','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1051','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',128,'EXECUTED','3:26c8ae0c53225f82d4c2a85c09ad9785','Create Index','',NULL,'2.0.5'),('20110915-1052','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',129,'EXECUTED','3:1efabdfd082ff2b0a34f570831f74ce5','Create Index','',NULL,'2.0.5'),('20110915-1053','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',130,'EXECUTED','3:00273104184bb4d2bb7155befc77efc3','Create Index','',NULL,'2.0.5'),('20110915-1103','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',131,'EXECUTED','3:29b35d66dc4168e03e1844296e309327','Create Index','',NULL,'2.0.5'),('20110915-1104','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',132,'EXECUTED','3:d63107017bdcef0e28d7ad5e4df21ae5','Create Index','',NULL,'2.0.5'),('20110915-1107','sunbiz','liquibase-core-data.xml','2016-07-07 08:14:29',10038,'EXECUTED','3:18eb4edef88534b45b384e6bc3ccce75','Insert Row','',NULL,'2.0.5'),('20110915-1133','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',133,'EXECUTED','3:fea4ad8ce44911eeaab8ac8c1cc9122d','Create Index','',NULL,'2.0.5'),('20110915-1135','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',350,'EXECUTED','3:f0bc11508a871044f5a572b7f8103d52','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1148','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',351,'EXECUTED','3:a5ef601dc184a85e988eded2f1f82dcb','Add Unique Constraint','',NULL,'2.0.5'),('20110915-1149','sunbiz','liquibase-core-data.xml','2016-07-07 08:14:29',10039,'EXECUTED','3:83534d43a9a9cc1ea3a80f1d5f5570af','Insert Row (x10)','',NULL,'2.0.5'),('20110915-1202','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',352,'EXECUTED','3:2c58f7f1e2450c60898bffe6933c9b34','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1203','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',353,'EXECUTED','3:5bce62082a32d3624854a198d3fa35b7','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1210','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',354,'EXECUTED','3:e17eee5b8c4bb236a0ea6e6ade5abed7','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1215','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',73,'EXECUTED','3:d772a6a8adedbb1c012dac58ffb221c3','Create Table','',NULL,'2.0.5'),('20110915-1222','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',78,'EXECUTED','3:25ce4e3219f2b8c85e06d47dfc097382','Create Table','',NULL,'2.0.5'),('20110915-1225','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',355,'EXECUTED','3:2d4f77176fd59955ff719c46ae8b0cfc','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1226','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',356,'EXECUTED','3:66155de3997745548dbca510649cd09d','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1227','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',357,'EXECUTED','3:6700b07595d6060269b86903d08bb2a5','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1231','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',79,'EXECUTED','3:e9f6104a25d8b37146b27e568b6e3d3f','Create Table','',NULL,'2.0.5'),('20110915-1240','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',358,'EXECUTED','3:5a30b62738cf57a4804310add8f71b6a','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1241','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',359,'EXECUTED','3:a48aa09c19549e43fc538a70380ae61f','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1242','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',360,'EXECUTED','3:e0e23621fabe23f3f04c4d13105d528c','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1243','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',361,'EXECUTED','3:1d15d848cefc39090e90f3ea78f3cedc','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1244','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',362,'EXECUTED','3:6c5b2018afd741a3c7e39c563212df57','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1245','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',363,'EXECUTED','3:9b5b112797deb6eddc9f0fc01254e378','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1246','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',364,'EXECUTED','3:290a8c07c70dd6a5fe85be2d747ff0d8','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1247','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:23',134,'EXECUTED','3:0644f13c7f4bb764d3b17ad160bd8d41','Create Index','',NULL,'2.0.5'),('20110915-1248','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',365,'EXECUTED','3:5b42d27a7c7edfeb021e1dcfed0f33b3','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1258','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',80,'EXECUTED','3:07687ca4ba9b942a862a41dd9026bc9d','Create Table','',NULL,'2.0.5'),('20110915-1301','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',366,'EXECUTED','3:ef3a47a3fdd809ef4269e9643add2abd','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1302','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',367,'EXECUTED','3:e36c12350ebfbd624bdc6a6599410c85','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1303','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',368,'EXECUTED','3:5917c5e09a3f6077b728a576cd9bacb3','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1307','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',81,'EXECUTED','3:957d888738541ed76dda53e222079fa3','Create Table','',NULL,'2.0.5'),('20110915-1311','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',392,'EXECUTED','3:e88c86892fafb2f897f72a85c66954c0','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1312','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',369,'EXECUTED','3:fe2641c56b27b429c1c4a150e1b9af18','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1313','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',370,'EXECUTED','3:5c7ab96d3967d1ce4e00ebe23f4c4f6e','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1314','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',371,'EXECUTED','3:22902323fcd541f18ca0cb4f38299cb4','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1315','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',372,'EXECUTED','3:dd0d198da3d5d01f93d9acc23e89d51c','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1316','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',373,'EXECUTED','3:f22027f3fc0b1a3a826dc5d810fcd936','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1317','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',374,'EXECUTED','3:68aa00c9f2faa61031d0b4544f4cb31b','Add Unique Constraint','',NULL,'2.0.5'),('20110915-1320','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',375,'EXECUTED','3:5d6a55ee33c33414cccc8b46776a36a4','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1323','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',376,'EXECUTED','3:4c3b84570d45b23d363f6ee76acd966f','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1325','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',82,'EXECUTED','3:0813953451c461376a6ab5a13e4654dd','Create Table','',NULL,'2.0.5'),('20110915-1327','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',377,'EXECUTED','3:3c8aaca28033c8a01e4bceb7421f8e8e','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1328','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',378,'EXECUTED','3:05b6e994f2a09b23826264d31f275b5e','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1329','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',379,'EXECUTED','3:40729ae012b9ed8bd55439b233ec10cc','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1337','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',83,'EXECUTED','3:06fd47a34713fad9678463bba9675496','Create Table','',NULL,'2.0.5'),('20110915-1342','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',380,'EXECUTED','3:bb52caf0ec6e80e24d6fc0c7f2c95631','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1343','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',381,'EXECUTED','3:b36c3436facfe7c9371f7780ebb8701d','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1344','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',382,'EXECUTED','3:010fa7bc125bcb8caa320d38a38a7e3f','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1345','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',383,'EXECUTED','3:e3cdd84f2e6632a4dd8c526cf9ff476e','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1346','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',384,'EXECUTED','3:7f6420b23addd5b33320e04adbc134a3','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1435','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',84,'EXECUTED','3:511f99d7cb13e5fc1112ccb4633e0e45','Create Table','',NULL,'2.0.5'),('20110915-1440','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',385,'EXECUTED','3:2cb254be6daeeebb74fc0e1d64728a62','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1441','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',386,'EXECUTED','3:8bd11d5102eff3b52b1d925e44627a48','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1442','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',387,'EXECUTED','3:4cf7afc33839c19f830e996e8546ea72','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1443','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',388,'EXECUTED','3:cf41f73f64c11150062b2e2254a56908','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1450','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',85,'EXECUTED','3:f9348bf7337d32ebbf98545857b5c8cc','Create Table','',NULL,'2.0.5'),('20110915-1451','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',389,'EXECUTED','3:d98c8bdaacf99764ab3319db03b48542','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1452','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',390,'EXECUTED','3:3a2e67fd1f0215b49711e7e8dccd370d','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1453','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:27',391,'EXECUTED','3:6b1b7fb75fedc196cf833f04e216b9b2','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1459','sunbiz','liquibase-core-data.xml','2016-07-07 08:14:29',10040,'EXECUTED','3:5faead5506cbcde69490fef985711d66','Custom Change','Inserting core concept map types',NULL,'2.0.5'),('20110915-1524','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',393,'EXECUTED','3:8d609018e78b744ce30e8907ead0bec0','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1528','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:22',86,'EXECUTED','3:e8a5555a214d7bb6f17eb2466f59d12b','Create Table','',NULL,'2.0.5'),('20110915-1530','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',394,'EXECUTED','3:ddc26a0bb350b6c744ed6ff813b5c108','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1531','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',395,'EXECUTED','3:e9fa5722ba00d9b55d813f0fc8e5f9f9','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1532','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',396,'EXECUTED','3:72f0f61a12a3eead113be1fdcabadb6f','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1533','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',397,'EXECUTED','3:0430d8eecce280786a66713abd0b3439','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1534','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',398,'EXECUTED','3:21b6cde828dbe885059ea714cda4f470','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1536','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',399,'EXECUTED','3:01868c1383e5c9c409282b50e67e878c','Add Foreign Key Constraint','',NULL,'2.0.5'),('20110915-1700','sunbiz','liquibase-schema-only.xml','2016-07-07 08:14:28',400,'EXECUTED','3:ba5b74aeacacec55a49d31074b7e5023','Insert Row (x18)','',NULL,'2.0.5'),('201109152336','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10433,'MARK_RAN','3:a84f855a1db7201e08900f8c7a3d7c5f','Update Data','Updating logging level global property',NULL,'2.0.5'),('20110919-0638','sunbiz','liquibase-update-to-latest.xml','2011-09-19 00:00:00',10015,'MARK_RAN','3:5e540b763c3a16e9d37aa6423b7f798f',NULL,NULL,NULL,NULL),('20110919-0639-void_empty_attributes','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10434,'EXECUTED','3:ccdbab987b09073fc146f3a4a5a9aee4','Custom SQL','Void all attributes that have empty string values.',NULL,'2.0.5'),('20110922-0551','sunbiz','liquibase-update-to-latest.xml','2016-07-07 08:14:50',10306,'MARK_RAN','3:ab9b55e5104645690a4e1c5e35124258','Modify Column','Changing global_property.property from varbinary to varchar',NULL,'2.0.5'),('20110926-1200','raff','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10396,'MARK_RAN','3:bf884233110a210b6ffcef826093cf9d','Custom SQL','Change all empty concept_source.hl7_code to NULL',NULL,'2.0.5'),('201109301703','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10405,'MARK_RAN','3:11456d3e6867f3b521fb35e6f51ebe5a','Update Data','Converting general address format (if applicable)',NULL,'2.0.5'),('201109301704','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10406,'MARK_RAN','3:d64afe121c9355f6bbe46258876ce759','Update Data','Converting Spain address format (if applicable)',NULL,'2.0.5'),('201109301705','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10407,'MARK_RAN','3:d3b0c8265ee27456dc0491ff5fe8ca01','Update Data','Converting Rwanda address format (if applicable)',NULL,'2.0.5'),('201109301706','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10408,'MARK_RAN','3:17d3a0900ca751d8ce775a12444c75bf','Update Data','Converting USA address format (if applicable)',NULL,'2.0.5'),('201109301707','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10409,'MARK_RAN','3:afbd6428d0007325426f3c4446de2e38','Update Data','Converting Kenya address format (if applicable)',NULL,'2.0.5'),('201109301708','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10410,'MARK_RAN','3:570c9234597b477e4feffbaac0469495','Update Data','Converting Lesotho address format (if applicable)',NULL,'2.0.5'),('201109301709','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10411,'MARK_RAN','3:20c95ae336f437b4e0c91be5919b7a2b','Update Data','Converting Malawi address format (if applicable)',NULL,'2.0.5'),('201109301710','suho','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10412,'MARK_RAN','3:b06d71b4c220c7feed9c5a6459bea98a','Update Data','Converting Tanzania address format (if applicable)',NULL,'2.0.5'),('201110051353-fix-visit-attribute-type-columns','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10392,'MARK_RAN','3:d779b41ab27dca879d593aa606016bf6','Add Column (x2)','Refactoring visit_attribute_type table (devs only)',NULL,'2.0.5'),('201110072042-fix-location-attribute-type-columns','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:54',10402,'MARK_RAN','3:2e32ce0f25391341c8855604f4f40654','Add Column (x2)','Refactoring location_attribute_type table (devs only)',NULL,'2.0.5'),('201110072043-fix-provider-attribute-type-columns','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10439,'MARK_RAN','3:31aa196adfe1689c1098c5f36d490902','Add Column (x2)','Refactoring provider_attribute_type table (devs only)',NULL,'2.0.5'),('20111008-0938-1','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10450,'EXECUTED','3:fe6d462ba1a7bd81f4865e472cc223ce','Add Column','Allow Global Properties to be typed',NULL,'2.0.5'),('20111008-0938-2','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10451,'EXECUTED','3:f831d92c11eb6cd6b334d86160db0b95','Add Column','Allow Global Properties to be typed',NULL,'2.0.5'),('20111008-0938-3','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10452,'EXECUTED','3:f7bd79dfed90d56053dc376b6b8ee7e3','Add Column','Allow Global Properties to be typed',NULL,'2.0.5'),('20111008-0938-4','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10453,'EXECUTED','3:65003bd1bf99ff0aa8e2947978c58053','Add Column','Allow Global Properties to be typed',NULL,'2.0.5'),('201110091820-a','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10454,'MARK_RAN','3:364a0c70d2adbff31babab6f60ed72e7','Add Column','Add xslt column back to the form table',NULL,'2.0.5'),('201110091820-b','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10455,'MARK_RAN','3:0b792bf39452f2e81e502a7a98f9f3df','Add Column','Add template column back to the form table',NULL,'2.0.5'),('201110091820-c','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10457,'MARK_RAN','3:f71680d95ecf870619671fb7f416e457','Rename Table','Rename form_resource table to preserve data; 20111010-1515 reference is for bleeding-edge developers and can be generally ignored',NULL,'2.0.5'),('20111010-1515','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10458,'EXECUTED','3:3ccdc9a3ecf811382a0c12825c0aeeb3','Create Table, Add Foreign Key Constraint, Add Unique Constraint','Creating form_resource table',NULL,'2.0.5'),('20111128-1601','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10460,'EXECUTED','3:12fa4687d149a2f17251e546d47369d6','Insert Row','Inserting Auto Close Visits Task into \'schedule_task_config\' table',NULL,'2.0.5'),('20111209-1400-deleting-non-existing-roles-from-role-role-table','raff','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10461,'EXECUTED','3:3d74c1dd987a12d916218d68032d726d','Custom SQL','Deleting non-existing roles from the role_role table',NULL,'2.0.5'),('20111214-1500-setting-super-user-gender','raff','liquibase-update-to-latest.xml','2016-07-07 08:14:55',10464,'EXECUTED','3:2c281abfe7beb51983db13c187c072f3','Custom SQL','Setting super user gender',NULL,'2.0.5'),('20111218-1830','abbas','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10465,'EXECUTED','3:5f096b88988f19d9d3e596c03fba2b90','Add Unique Constraint, Add Column (x6), Add Foreign Key Constraint (x2)','Add unique uuid constraint and attributes inherited from BaseOpenmrsData to the person_merge_log table',NULL,'2.0.5'),('20111219-1404','bwolfe','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10466,'EXECUTED','3:3f8cfa9c088a103788bcf70de3ffaa8b','Update Data','Fix empty descriptions on relationship types',NULL,'2.0.5'),('20111222-1659','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10468,'EXECUTED','3:990b494647720b680efeefbab2c502de','Create Table, Create Index','Create clob_datatype_storage table',NULL,'2.0.5'),('201118012301','lkellett','liquibase-update-to-latest.xml','2016-07-07 08:14:53',10370,'MARK_RAN','3:0d96c10c52335339b1003e6dd933ccc2','Add Column','Adding the discontinued_reason_non_coded column to orders.',NULL,'2.0.5'),('201202020847','abbas','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10467,'EXECUTED','3:35bf2f2481ee34975e57f08d933583be','Modify data type, Add Not-Null Constraint','Change merged_data column type to CLOB in person_merge_log table',NULL,'2.0.5'),('20120330-0954','jkeiper','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10472,'EXECUTED','3:9c6084b4407395205fa39b34630d3522','Modify data type','Increase size of drug name column to 255 characters',NULL,'2.0.5'),('20120503-djmod','dkayiwa and djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10475,'EXECUTED','3:d31ac18d3a40e45c0ebb399c5d116951','Create Table, Add Foreign Key Constraint (x2)','Create test_order table',NULL,'2.0.5'),('20120613-0930','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10473,'EXECUTED','3:fe6387773a70b574b106b37686a8e8d3','Drop Not-Null Constraint','Dropping not null constraint from provider.identifier column',NULL,'2.0.5'),('20121007-orders_urgency','djazayeri','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10474,'EXECUTED','3:f8eb2228ea34f43ae21bedf4abc8736b','Add Column','Adding urgency column to orders table',NULL,'2.0.5'),('20121016-1504','wyclif','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10476,'EXECUTED','3:88db1819c0e9da738ed9332b5de73609','Drop Foreign Key Constraint, Modify Column, Add Foreign Key Constraint','Removing auto increment from test_order.order_id column',NULL,'2.0.5'),('20121020-TRUNK-3610','lluismf','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10477,'EXECUTED','3:a3159e65647f0ff1b667104012b5f4f0','Update Data (x2)','Rename global property autoCloseVisits.visitType to visits.autoCloseVisitType',NULL,'2.0.5'),('201306141103-TRUNK-3884','susantan','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10478,'EXECUTED','3:9581f8d869e69d911f04e48591a297d0','Add Foreign Key Constraint (x3)','Adding 3 foreign key relationships (creator,created_by,voided_by) to encounter_provider table',NULL,'2.0.5'),('20130925-TRUNK-4105','hannes','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10479,'EXECUTED','3:e81f96e97c307c2d265bce32a046d0ca','Create Index','Adding index on concept_reference_term.code column',NULL,'2.0.5'),('201401040436-TRUNK-3919','dkithmal','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10482,'EXECUTED','3:2fde2a2c0d2a917cd7ec0dfc990b96ac','Add Column, Add Foreign Key Constraint','Add changed_by column to location_tag table',NULL,'2.0.5'),('201401040438-TRUNK-3919','dkithmal','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10483,'EXECUTED','3:134b00185f7b61d7d6cccb66717dc4ae','Add Column','Add date_changed column to location_tag table',NULL,'2.0.5'),('201401040440-TRUNK-3919','dkithmal','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10484,'EXECUTED','3:2f3b927e3554c31abebb064835da2efc','Add Column, Add Foreign Key Constraint','Add changed_by column to location table',NULL,'2.0.5'),('201401040442-TRUNK-3919','dkithmal','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10485,'EXECUTED','3:6cf087a28acd99c02c00fd719a26e73b','Add Column','Add date_changed column to location table',NULL,'2.0.5'),('20140331-1-TRUNK-4335','rkorytkowski','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10480,'EXECUTED','3:abfcdbe1adf81de731e3997bd4a66818','Add Column','Adding uuid to concept_set_derived',NULL,'2.0.5'),('20140331-2-TRUNK-4335','rkorytkowski','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10481,'EXECUTED','3:2e11785b7fb8a32248f56f7ba55fc143','Add Column','Adding uuid to drug_ingredient',NULL,'2.0.5'),('20150122-1414','rkorytkowski','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10332,'EXECUTED','3:b662cac19085b837d902916f20a27da9','Update Data','Reverting concept name type to NULL for concepts having names tagged as default',NULL,'2.0.5'),('20150122-1420','rkorytkowski','liquibase-update-to-latest.xml','2016-07-07 08:14:51',10333,'EXECUTED','3:c75793ade72db0648a912f22f79cf461','Update Data, Delete Data (x2)','Setting concept name type to fully specified for names tagged as default',NULL,'2.0.5'),('3-increase-privilege-col-size-person_attribute_type','dkayiwa','liquibase-update-to-latest.xml','2016-07-07 08:14:56',10471,'EXECUTED','3:c2465b2417463d93f1101c6683f41250','Drop Foreign Key Constraint, Modify Column, Add Foreign Key Constraint','Increasing the size of the edit_privilege column in the person_attribute_type table',NULL,'2.0.5'),('create-logic-rule-definition','mseaton','liquibase.xml','2016-07-07 08:15:05',10490,'EXECUTED','3:5327271907425ea8182024723912460c','Create Table, Create Index (x3), Add Foreign Key Constraint (x3)','',NULL,'2.0.5'),('create-logic-rule-token-tag','nribeka','liquibase.xml','2016-07-07 08:15:05',10489,'EXECUTED','3:0d0c10ea14371337b4b2a8da0972d768','Create Table, Add Foreign Key Constraint','',NULL,'2.0.5'),('create-logic_token_registration','djazayeri','liquibase.xml','2016-07-07 08:15:05',10491,'EXECUTED','3:fba04de00a55b2d5478aed1653df7007','Create Table, Add Foreign Key Constraint (x2)','',NULL,'2.0.5'),('create-logic_token_registration_tag','djazayeri','liquibase.xml','2016-07-07 08:15:05',10492,'EXECUTED','3:e941337ae6d3dfb33bf3ff92aeb63b89','Create Table, Add Foreign Key Constraint','',NULL,'2.0.5'),('create_logic_rule_token','nribeka','liquibase.xml','2016-07-07 08:15:05',10488,'EXECUTED','3:5c95d01a824456e85a6729745a0e814d','Create Table, Add Foreign Key Constraint (x2)','',NULL,'2.0.5'),('disable-foreign-key-checks','ben','liquibase-core-data.xml','2016-07-07 08:14:28',10017,'EXECUTED','3:cc124077cda1cfb0c70c1ec823551223','Custom SQL','',NULL,'2.0.5'),('drop_logic_rule_token','nribeka','liquibase.xml','2016-07-07 08:15:05',10487,'MARK_RAN','3:4bfc38917a898cfc54c440e045cd655c','Drop Table','',NULL,'2.0.5'),('drop_logic_rule_token_tag','nribeka','liquibase.xml','2016-07-07 08:15:05',10486,'MARK_RAN','3:4ba9ee4a3873ff8c847a3d5fb7275732','Drop Table','',NULL,'2.0.5'),('enable-foreign-key-checks','ben','liquibase-core-data.xml','2016-07-07 08:14:29',10041,'EXECUTED','3:fcfe4902a8f3eda10332567a1a51cb49','Custom SQL','',NULL,'2.0.5');
/*!40000 ALTER TABLE `liquibasechangelog` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Add missing references to two more change sets from `openmrs-core/api/src/main/resources/org/openmrs/liquibase/snapshots/schema-only/liquibase-schema-only-1.9.x.xml` to the table `liquibasechangelog`.
--
-- openmrs-core/api uses Liquibase snapshots since 2.4.0 and without those references updating the OpenMRS database fails.
--

LOCK TABLES `liquibasechangelog` WRITE;
/*!40000 ALTER TABLE `liquibasechangelog` DISABLE KEYS */;
INSERT INTO `liquibasechangelog` VALUES ('20120529-2230','mvorobey','liquibase-schema-only.xml','2016-07-07 08:14:28',401,'EXECUTED',NULL,'Add Foreign Key Constraint','',NULL,'2.0.5'),('20120529-2231','mvorobey','liquibase-schema-only.xml','2016-07-07 08:14:28',402,'EXECUTED',NULL,'Add Foreign Key Constraint','',NULL,'2.0.5');
/*!40000 ALTER TABLE `liquibasechangelog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `liquibasechangeloglock`
--

DROP TABLE IF EXISTS `liquibasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `liquibasechangeloglock` (
  `ID` int(11) NOT NULL,
  `LOCKED` tinyint(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `liquibasechangeloglock`
--

LOCK TABLES `liquibasechangeloglock` WRITE;
/*!40000 ALTER TABLE `liquibasechangeloglock` DISABLE KEYS */;
INSERT INTO `liquibasechangeloglock` VALUES (1,0,NULL,NULL);
/*!40000 ALTER TABLE `liquibasechangeloglock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location`
--

DROP TABLE IF EXISTS `location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `location` (
  `location_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` varchar(255) DEFAULT NULL,
  `address1` varchar(255) DEFAULT NULL,
  `address2` varchar(255) DEFAULT NULL,
  `city_village` varchar(255) DEFAULT NULL,
  `state_province` varchar(255) DEFAULT NULL,
  `postal_code` varchar(50) DEFAULT NULL,
  `country` varchar(50) DEFAULT NULL,
  `latitude` varchar(50) DEFAULT NULL,
  `longitude` varchar(50) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `county_district` varchar(255) DEFAULT NULL,
  `address3` varchar(255) DEFAULT NULL,
  `address4` varchar(255) DEFAULT NULL,
  `address5` varchar(255) DEFAULT NULL,
  `address6` varchar(255) DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `parent_location` int(11) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  PRIMARY KEY (`location_id`),
  UNIQUE KEY `location_uuid_index` (`uuid`),
  KEY `name_of_location` (`name`),
  KEY `location_retired_status` (`retired`),
  KEY `user_who_created_location` (`creator`),
  KEY `user_who_retired_location` (`retired_by`),
  KEY `parent_location` (`parent_location`),
  KEY `location_changed_by` (`changed_by`),
  CONSTRAINT `location_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `parent_location` FOREIGN KEY (`parent_location`) REFERENCES `location` (`location_id`),
  CONSTRAINT `user_who_created_location` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_location` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location`
--

LOCK TABLES `location` WRITE;
/*!40000 ALTER TABLE `location` DISABLE KEYS */;
INSERT INTO `location` VALUES (1,'Unknown Location',NULL,'','','','','','',NULL,NULL,1,'2005-09-22 00:00:00',NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'8d6c993e-c2cc-11de-8d13-0010c6dffd0f',NULL,NULL);
/*!40000 ALTER TABLE `location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_attribute`
--

DROP TABLE IF EXISTS `location_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `location_attribute` (
  `location_attribute_id` int(11) NOT NULL AUTO_INCREMENT,
  `location_id` int(11) NOT NULL,
  `attribute_type_id` int(11) NOT NULL,
  `value_reference` text NOT NULL,
  `uuid` char(38) NOT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`location_attribute_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `location_attribute_location_fk` (`location_id`),
  KEY `location_attribute_attribute_type_id_fk` (`attribute_type_id`),
  KEY `location_attribute_creator_fk` (`creator`),
  KEY `location_attribute_changed_by_fk` (`changed_by`),
  KEY `location_attribute_voided_by_fk` (`voided_by`),
  CONSTRAINT `location_attribute_attribute_type_id_fk` FOREIGN KEY (`attribute_type_id`) REFERENCES `location_attribute_type` (`location_attribute_type_id`),
  CONSTRAINT `location_attribute_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `location_attribute_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `location_attribute_location_fk` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
  CONSTRAINT `location_attribute_voided_by_fk` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_attribute`
--

LOCK TABLES `location_attribute` WRITE;
/*!40000 ALTER TABLE `location_attribute` DISABLE KEYS */;
/*!40000 ALTER TABLE `location_attribute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_attribute_type`
--

DROP TABLE IF EXISTS `location_attribute_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `location_attribute_type` (
  `location_attribute_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `datatype` varchar(255) DEFAULT NULL,
  `datatype_config` text,
  `preferred_handler` varchar(255) DEFAULT NULL,
  `handler_config` text,
  `min_occurs` int(11) NOT NULL,
  `max_occurs` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`location_attribute_type_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `location_attribute_type_creator_fk` (`creator`),
  KEY `location_attribute_type_changed_by_fk` (`changed_by`),
  KEY `location_attribute_type_retired_by_fk` (`retired_by`),
  CONSTRAINT `location_attribute_type_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `location_attribute_type_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `location_attribute_type_retired_by_fk` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_attribute_type`
--

LOCK TABLES `location_attribute_type` WRITE;
/*!40000 ALTER TABLE `location_attribute_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `location_attribute_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_tag`
--

DROP TABLE IF EXISTS `location_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `location_tag` (
  `location_tag_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  PRIMARY KEY (`location_tag_id`),
  UNIQUE KEY `location_tag_uuid_index` (`uuid`),
  KEY `location_tag_creator` (`creator`),
  KEY `location_tag_retired_by` (`retired_by`),
  KEY `location_tag_changed_by` (`changed_by`),
  CONSTRAINT `location_tag_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `location_tag_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `location_tag_retired_by` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_tag`
--

LOCK TABLES `location_tag` WRITE;
/*!40000 ALTER TABLE `location_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `location_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_tag_map`
--

DROP TABLE IF EXISTS `location_tag_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `location_tag_map` (
  `location_id` int(11) NOT NULL,
  `location_tag_id` int(11) NOT NULL,
  PRIMARY KEY (`location_id`,`location_tag_id`),
  KEY `location_tag_map_tag` (`location_tag_id`),
  CONSTRAINT `location_tag_map_location` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
  CONSTRAINT `location_tag_map_tag` FOREIGN KEY (`location_tag_id`) REFERENCES `location_tag` (`location_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_tag_map`
--

LOCK TABLES `location_tag_map` WRITE;
/*!40000 ALTER TABLE `location_tag_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `location_tag_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `logic_rule_definition`
--

DROP TABLE IF EXISTS `logic_rule_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logic_rule_definition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` char(38) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `rule_content` varchar(2048) NOT NULL,
  `language` varchar(255) NOT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL DEFAULT '0002-11-30 00:00:00',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` smallint(6) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `creator_idx` (`creator`),
  KEY `changed_by_idx` (`changed_by`),
  KEY `retired_by_idx` (`retired_by`),
  CONSTRAINT `changed_by_for_rule_definition` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `creator_for_rule_definition` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `retired_by_for_rule_definition` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `logic_rule_definition`
--

LOCK TABLES `logic_rule_definition` WRITE;
/*!40000 ALTER TABLE `logic_rule_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `logic_rule_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `logic_rule_token`
--

DROP TABLE IF EXISTS `logic_rule_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logic_rule_token` (
  `logic_rule_token_id` int(11) NOT NULL AUTO_INCREMENT,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL DEFAULT '0002-11-30 00:00:00',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `token` varchar(512) NOT NULL,
  `class_name` varchar(512) NOT NULL,
  `state` varchar(512) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`logic_rule_token_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `token_creator` (`creator`),
  KEY `token_changed_by` (`changed_by`),
  CONSTRAINT `token_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `person` (`person_id`),
  CONSTRAINT `token_creator` FOREIGN KEY (`creator`) REFERENCES `person` (`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `logic_rule_token`
--

LOCK TABLES `logic_rule_token` WRITE;
/*!40000 ALTER TABLE `logic_rule_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `logic_rule_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `logic_rule_token_tag`
--

DROP TABLE IF EXISTS `logic_rule_token_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logic_rule_token_tag` (
  `logic_rule_token_id` int(11) NOT NULL,
  `tag` varchar(512) NOT NULL,
  KEY `token_tag` (`logic_rule_token_id`),
  CONSTRAINT `token_tag` FOREIGN KEY (`logic_rule_token_id`) REFERENCES `logic_rule_token` (`logic_rule_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `logic_rule_token_tag`
--

LOCK TABLES `logic_rule_token_tag` WRITE;
/*!40000 ALTER TABLE `logic_rule_token_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `logic_rule_token_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `logic_token_registration`
--

DROP TABLE IF EXISTS `logic_token_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logic_token_registration` (
  `token_registration_id` int(11) NOT NULL AUTO_INCREMENT,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL DEFAULT '0002-11-30 00:00:00',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `token` varchar(512) NOT NULL,
  `provider_class_name` varchar(512) NOT NULL,
  `provider_token` varchar(512) NOT NULL,
  `configuration` varchar(2000) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`token_registration_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `token_registration_creator` (`creator`),
  KEY `token_registration_changed_by` (`changed_by`),
  CONSTRAINT `token_registration_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `token_registration_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `logic_token_registration`
--

LOCK TABLES `logic_token_registration` WRITE;
/*!40000 ALTER TABLE `logic_token_registration` DISABLE KEYS */;
INSERT INTO `logic_token_registration` VALUES (1,2,'2016-07-07 08:15:10',NULL,NULL,'encounterLocation','org.openmrs.logic.datasource.EncounterDataSource','encounterLocation','encounterLocation','b06d3ddb-170e-4e17-a04a-d8d1cd349a4d'),(2,2,'2016-07-07 08:15:10',NULL,NULL,'encounterProvider','org.openmrs.logic.datasource.EncounterDataSource','encounterProvider','encounterProvider','2fff4388-7ca7-4686-ac66-f38090048ff9'),(3,2,'2016-07-07 08:15:10',NULL,NULL,'encounter','org.openmrs.logic.datasource.EncounterDataSource','encounter','encounter','efee7046-c86f-4aba-b6f6-e544f9077e02'),(4,2,'2016-07-07 08:15:10',NULL,NULL,'identifier','org.openmrs.logic.datasource.PatientDataSource','identifier','identifier','815c7955-dd76-4226-ac32-b08496b373c6'),(5,2,'2016-07-07 08:15:10',NULL,NULL,'family name','org.openmrs.logic.datasource.PersonDataSource','family name','family name','50606952-2e58-4b62-8650-9a58192fb0b8'),(6,2,'2016-07-07 08:15:10',NULL,NULL,'middle name','org.openmrs.logic.datasource.PersonDataSource','middle name','middle name','ecfe6086-d4a3-4236-bdf9-261d4ea39dab'),(7,2,'2016-07-07 08:15:10',NULL,NULL,'death date','org.openmrs.logic.datasource.PersonDataSource','death date','death date','bb3b8e24-ad31-4737-a90d-1b6e10ce0e74'),(8,2,'2016-07-07 08:15:10',NULL,NULL,'birthdate','org.openmrs.logic.datasource.PersonDataSource','birthdate','birthdate','f8b85cf7-2260-4902-a8fa-6d0c77189661'),(9,2,'2016-07-07 08:15:10',NULL,NULL,'cause of death','org.openmrs.logic.datasource.PersonDataSource','cause of death','cause of death','cc36441e-2b2f-4450-8fac-f0f85e73ef3f'),(10,2,'2016-07-07 08:15:10',NULL,NULL,'birthdate estimated','org.openmrs.logic.datasource.PersonDataSource','birthdate estimated','birthdate estimated','d0c05d58-c54d-40f7-bd0d-d21cabb81c4f'),(11,2,'2016-07-07 08:15:10',NULL,NULL,'gender','org.openmrs.logic.datasource.PersonDataSource','gender','gender','d7da980c-4f63-4ca5-b0f5-ed5e40f44ba1'),(12,2,'2016-07-07 08:15:10',NULL,NULL,'family name2','org.openmrs.logic.datasource.PersonDataSource','family name2','family name2','f1889d0e-c043-40cd-b30d-45546c9d8220'),(13,2,'2016-07-07 08:15:10',NULL,NULL,'dead','org.openmrs.logic.datasource.PersonDataSource','dead','dead','4c098e08-e1fd-417f-b3ea-e9b5e500cb95'),(14,2,'2016-07-07 08:15:10',NULL,NULL,'given name','org.openmrs.logic.datasource.PersonDataSource','given name','given name','29555b7d-c71e-4e14-8773-75b0dbbb2da0'),(15,2,'2016-07-07 08:15:10',NULL,NULL,'CURRENT STATE','org.openmrs.logic.datasource.ProgramDataSource','CURRENT STATE','CURRENT STATE','167efad0-8f0a-44f9-9f33-24663eaa22e9'),(16,2,'2016-07-07 08:15:10',NULL,NULL,'PROGRAM ENROLLMENT','org.openmrs.logic.datasource.ProgramDataSource','PROGRAM ENROLLMENT','PROGRAM ENROLLMENT','8c77f6a6-d183-47af-ab5d-b97aea4d9ca7'),(17,2,'2016-07-07 08:15:10',NULL,NULL,'PROGRAM COMPLETION','org.openmrs.logic.datasource.ProgramDataSource','PROGRAM COMPLETION','PROGRAM COMPLETION','fe9e859f-e86f-4bde-a2b1-66f2da8ec842');
/*!40000 ALTER TABLE `logic_token_registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `logic_token_registration_tag`
--

DROP TABLE IF EXISTS `logic_token_registration_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logic_token_registration_tag` (
  `token_registration_id` int(11) NOT NULL,
  `tag` varchar(512) NOT NULL,
  KEY `token_registration_tag` (`token_registration_id`),
  CONSTRAINT `token_registration_tag` FOREIGN KEY (`token_registration_id`) REFERENCES `logic_token_registration` (`token_registration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `logic_token_registration_tag`
--

LOCK TABLES `logic_token_registration_tag` WRITE;
/*!40000 ALTER TABLE `logic_token_registration_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `logic_token_registration_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `note`
--

DROP TABLE IF EXISTS `note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note` (
  `note_id` int(11) NOT NULL DEFAULT '0',
  `note_type` varchar(50) DEFAULT NULL,
  `patient_id` int(11) DEFAULT NULL,
  `obs_id` int(11) DEFAULT NULL,
  `encounter_id` int(11) DEFAULT NULL,
  `text` text NOT NULL,
  `priority` int(11) DEFAULT NULL,
  `parent` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`note_id`),
  UNIQUE KEY `note_uuid_index` (`uuid`),
  KEY `user_who_changed_note` (`changed_by`),
  KEY `user_who_created_note` (`creator`),
  KEY `encounter_note` (`encounter_id`),
  KEY `obs_note` (`obs_id`),
  KEY `note_hierarchy` (`parent`),
  KEY `patient_note` (`patient_id`),
  CONSTRAINT `encounter_note` FOREIGN KEY (`encounter_id`) REFERENCES `encounter` (`encounter_id`),
  CONSTRAINT `note_hierarchy` FOREIGN KEY (`parent`) REFERENCES `note` (`note_id`),
  CONSTRAINT `obs_note` FOREIGN KEY (`obs_id`) REFERENCES `obs` (`obs_id`),
  CONSTRAINT `patient_note` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON UPDATE CASCADE,
  CONSTRAINT `user_who_changed_note` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_created_note` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `note`
--

LOCK TABLES `note` WRITE;
/*!40000 ALTER TABLE `note` DISABLE KEYS */;
/*!40000 ALTER TABLE `note` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_alert`
--

DROP TABLE IF EXISTS `notification_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_alert` (
  `alert_id` int(11) NOT NULL AUTO_INCREMENT,
  `text` varchar(512) NOT NULL,
  `satisfied_by_any` tinyint(1) NOT NULL DEFAULT '0',
  `alert_read` tinyint(1) NOT NULL DEFAULT '0',
  `date_to_expire` datetime DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`alert_id`),
  UNIQUE KEY `notification_alert_uuid_index` (`uuid`),
  KEY `alert_date_to_expire_idx` (`date_to_expire`),
  KEY `user_who_changed_alert` (`changed_by`),
  KEY `alert_creator` (`creator`),
  CONSTRAINT `alert_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_changed_alert` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_alert`
--

LOCK TABLES `notification_alert` WRITE;
/*!40000 ALTER TABLE `notification_alert` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_alert_recipient`
--

DROP TABLE IF EXISTS `notification_alert_recipient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_alert_recipient` (
  `alert_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `alert_read` tinyint(1) NOT NULL DEFAULT '0',
  `date_changed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`alert_id`,`user_id`),
  KEY `alert_read_by_user` (`user_id`),
  CONSTRAINT `alert_read_by_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `id_of_alert` FOREIGN KEY (`alert_id`) REFERENCES `notification_alert` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_alert_recipient`
--

LOCK TABLES `notification_alert_recipient` WRITE;
/*!40000 ALTER TABLE `notification_alert_recipient` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_alert_recipient` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_template`
--

DROP TABLE IF EXISTS `notification_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_template` (
  `template_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `template` text,
  `subject` varchar(100) DEFAULT NULL,
  `sender` varchar(255) DEFAULT NULL,
  `recipients` varchar(512) DEFAULT NULL,
  `ordinal` int(11) DEFAULT '0',
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `notification_template_uuid_index` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_template`
--

LOCK TABLES `notification_template` WRITE;
/*!40000 ALTER TABLE `notification_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `obs`
--

DROP TABLE IF EXISTS `obs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `obs` (
  `obs_id` int(11) NOT NULL AUTO_INCREMENT,
  `person_id` int(11) NOT NULL,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `encounter_id` int(11) DEFAULT NULL,
  `order_id` int(11) DEFAULT NULL,
  `obs_datetime` datetime NOT NULL,
  `location_id` int(11) DEFAULT NULL,
  `obs_group_id` int(11) DEFAULT NULL,
  `accession_number` varchar(255) DEFAULT NULL,
  `value_group_id` int(11) DEFAULT NULL,
  `value_boolean` tinyint(1) DEFAULT NULL,
  `value_coded` int(11) DEFAULT NULL,
  `value_coded_name_id` int(11) DEFAULT NULL,
  `value_drug` int(11) DEFAULT NULL,
  `value_datetime` datetime DEFAULT NULL,
  `value_numeric` double DEFAULT NULL,
  `value_modifier` varchar(2) DEFAULT NULL,
  `value_text` text,
  `value_complex` varchar(255) DEFAULT NULL,
  `comments` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  `previous_version` int(11) DEFAULT NULL,
  PRIMARY KEY (`obs_id`),
  UNIQUE KEY `obs_uuid_index` (`uuid`),
  KEY `obs_datetime_idx` (`obs_datetime`),
  KEY `obs_concept` (`concept_id`),
  KEY `obs_enterer` (`creator`),
  KEY `encounter_observations` (`encounter_id`),
  KEY `obs_location` (`location_id`),
  KEY `obs_grouping_id` (`obs_group_id`),
  KEY `obs_order` (`order_id`),
  KEY `person_obs` (`person_id`),
  KEY `answer_concept` (`value_coded`),
  KEY `obs_name_of_coded_value` (`value_coded_name_id`),
  KEY `answer_concept_drug` (`value_drug`),
  KEY `user_who_voided_obs` (`voided_by`),
  KEY `previous_version` (`previous_version`),
  CONSTRAINT `answer_concept` FOREIGN KEY (`value_coded`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `answer_concept_drug` FOREIGN KEY (`value_drug`) REFERENCES `drug` (`drug_id`),
  CONSTRAINT `encounter_observations` FOREIGN KEY (`encounter_id`) REFERENCES `encounter` (`encounter_id`),
  CONSTRAINT `obs_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `obs_enterer` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `obs_grouping_id` FOREIGN KEY (`obs_group_id`) REFERENCES `obs` (`obs_id`),
  CONSTRAINT `obs_location` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
  CONSTRAINT `obs_name_of_coded_value` FOREIGN KEY (`value_coded_name_id`) REFERENCES `concept_name` (`concept_name_id`),
  CONSTRAINT `obs_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `person_obs` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`) ON UPDATE CASCADE,
  CONSTRAINT `previous_version` FOREIGN KEY (`previous_version`) REFERENCES `obs` (`obs_id`),
  CONSTRAINT `user_who_voided_obs` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `obs`
--

LOCK TABLES `obs` WRITE;
/*!40000 ALTER TABLE `obs` DISABLE KEYS */;
/*!40000 ALTER TABLE `obs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_type`
--

DROP TABLE IF EXISTS `order_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_type` (
  `order_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`order_type_id`),
  UNIQUE KEY `order_type_uuid_index` (`uuid`),
  KEY `order_type_retired_status` (`retired`),
  KEY `type_created_by` (`creator`),
  KEY `user_who_retired_order_type` (`retired_by`),
  CONSTRAINT `type_created_by` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_order_type` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_type`
--

LOCK TABLES `order_type` WRITE;
/*!40000 ALTER TABLE `order_type` DISABLE KEYS */;
INSERT INTO `order_type` VALUES (2,'Drug Order','An order for a medication to be given to the patient',1,'2010-05-12 00:00:00',0,NULL,NULL,NULL,'131168f4-15f5-102d-96e4-000c29c2a5d7');
/*!40000 ALTER TABLE `order_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `orders` (
  `order_id` int(11) NOT NULL AUTO_INCREMENT,
  `order_type_id` int(11) NOT NULL DEFAULT '0',
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `orderer` int(11) DEFAULT '0',
  `encounter_id` int(11) DEFAULT NULL,
  `instructions` text,
  `start_date` datetime DEFAULT NULL,
  `auto_expire_date` datetime DEFAULT NULL,
  `discontinued` tinyint(1) NOT NULL DEFAULT '0',
  `discontinued_date` datetime DEFAULT NULL,
  `discontinued_by` int(11) DEFAULT NULL,
  `discontinued_reason` int(11) DEFAULT NULL,
  `discontinued_reason_non_coded` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `patient_id` int(11) NOT NULL,
  `accession_number` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  `urgency` varchar(50) NOT NULL DEFAULT 'ROUTINE',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `orders_uuid_index` (`uuid`),
  KEY `order_creator` (`creator`),
  KEY `user_who_discontinued_order` (`discontinued_by`),
  KEY `discontinued_because` (`discontinued_reason`),
  KEY `orders_in_encounter` (`encounter_id`),
  KEY `type_of_order` (`order_type_id`),
  KEY `orderer_not_drug` (`orderer`),
  KEY `order_for_patient` (`patient_id`),
  KEY `user_who_voided_order` (`voided_by`),
  CONSTRAINT `discontinued_because` FOREIGN KEY (`discontinued_reason`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `order_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `order_for_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON UPDATE CASCADE,
  CONSTRAINT `orderer_not_drug` FOREIGN KEY (`orderer`) REFERENCES `users` (`user_id`),
  CONSTRAINT `orders_in_encounter` FOREIGN KEY (`encounter_id`) REFERENCES `encounter` (`encounter_id`),
  CONSTRAINT `type_of_order` FOREIGN KEY (`order_type_id`) REFERENCES `order_type` (`order_type_id`),
  CONSTRAINT `user_who_discontinued_order` FOREIGN KEY (`discontinued_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_order` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient`
--

DROP TABLE IF EXISTS `patient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `patient` (
  `patient_id` int(11) NOT NULL,
  `tribe` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`patient_id`),
  KEY `user_who_changed_pat` (`changed_by`),
  KEY `user_who_created_patient` (`creator`),
  KEY `user_who_voided_patient` (`voided_by`),
  CONSTRAINT `person_id_for_patient` FOREIGN KEY (`patient_id`) REFERENCES `person` (`person_id`) ON UPDATE CASCADE,
  CONSTRAINT `user_who_changed_pat` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_created_patient` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_patient` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient`
--

LOCK TABLES `patient` WRITE;
/*!40000 ALTER TABLE `patient` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_identifier`
--

DROP TABLE IF EXISTS `patient_identifier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `patient_identifier` (
  `patient_identifier_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_id` int(11) NOT NULL DEFAULT '0',
  `identifier` varchar(50) NOT NULL DEFAULT '',
  `identifier_type` int(11) NOT NULL DEFAULT '0',
  `preferred` tinyint(1) NOT NULL DEFAULT '0',
  `location_id` int(11) DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `date_changed` datetime DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`patient_identifier_id`),
  UNIQUE KEY `patient_identifier_uuid_index` (`uuid`),
  KEY `identifier_name` (`identifier`),
  KEY `idx_patient_identifier_patient` (`patient_id`),
  KEY `identifier_creator` (`creator`),
  KEY `defines_identifier_type` (`identifier_type`),
  KEY `patient_identifier_ibfk_2` (`location_id`),
  KEY `identifier_voider` (`voided_by`),
  KEY `patient_identifier_changed_by` (`changed_by`),
  CONSTRAINT `defines_identifier_type` FOREIGN KEY (`identifier_type`) REFERENCES `patient_identifier_type` (`patient_identifier_type_id`),
  CONSTRAINT `identifier_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `identifier_voider` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `patient_identifier_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `patient_identifier_ibfk_2` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_identifier`
--

LOCK TABLES `patient_identifier` WRITE;
/*!40000 ALTER TABLE `patient_identifier` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient_identifier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_identifier_type`
--

DROP TABLE IF EXISTS `patient_identifier_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `patient_identifier_type` (
  `patient_identifier_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL DEFAULT '',
  `description` text NOT NULL,
  `format` varchar(255) DEFAULT NULL,
  `check_digit` tinyint(1) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `required` tinyint(1) NOT NULL DEFAULT '0',
  `format_description` varchar(255) DEFAULT NULL,
  `validator` varchar(200) DEFAULT NULL,
  `location_behavior` varchar(50) DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`patient_identifier_type_id`),
  UNIQUE KEY `patient_identifier_type_uuid_index` (`uuid`),
  KEY `patient_identifier_type_retired_status` (`retired`),
  KEY `type_creator` (`creator`),
  KEY `user_who_retired_patient_identifier_type` (`retired_by`),
  CONSTRAINT `type_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_patient_identifier_type` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_identifier_type`
--

LOCK TABLES `patient_identifier_type` WRITE;
/*!40000 ALTER TABLE `patient_identifier_type` DISABLE KEYS */;
INSERT INTO `patient_identifier_type` VALUES (1,'OpenMRS Identification Number','Unique number used in OpenMRS','',1,1,'2005-09-22 00:00:00',0,NULL,'org.openmrs.patient.impl.LuhnIdentifierValidator',NULL,0,NULL,NULL,NULL,'8d793bee-c2cc-11de-8d13-0010c6dffd0f'),(2,'Old Identification Number','Number given out prior to the OpenMRS system (No check digit)','',0,1,'2005-09-22 00:00:00',0,NULL,NULL,NULL,0,NULL,NULL,NULL,'8d79403a-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `patient_identifier_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_program`
--

DROP TABLE IF EXISTS `patient_program`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `patient_program` (
  `patient_program_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_id` int(11) NOT NULL DEFAULT '0',
  `program_id` int(11) NOT NULL DEFAULT '0',
  `date_enrolled` datetime DEFAULT NULL,
  `date_completed` datetime DEFAULT NULL,
  `location_id` int(11) DEFAULT NULL,
  `outcome_concept_id` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`patient_program_id`),
  UNIQUE KEY `patient_program_uuid_index` (`uuid`),
  KEY `user_who_changed` (`changed_by`),
  KEY `patient_program_creator` (`creator`),
  KEY `patient_in_program` (`patient_id`),
  KEY `program_for_patient` (`program_id`),
  KEY `user_who_voided_patient_program` (`voided_by`),
  KEY `patient_program_location_id` (`location_id`),
  KEY `patient_program_outcome_concept_id_fk` (`outcome_concept_id`),
  CONSTRAINT `patient_in_program` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON UPDATE CASCADE,
  CONSTRAINT `patient_program_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `patient_program_location_id` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
  CONSTRAINT `patient_program_outcome_concept_id_fk` FOREIGN KEY (`outcome_concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `program_for_patient` FOREIGN KEY (`program_id`) REFERENCES `program` (`program_id`),
  CONSTRAINT `user_who_changed` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_patient_program` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_program`
--

LOCK TABLES `patient_program` WRITE;
/*!40000 ALTER TABLE `patient_program` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient_program` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_state`
--

DROP TABLE IF EXISTS `patient_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `patient_state` (
  `patient_state_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_program_id` int(11) NOT NULL DEFAULT '0',
  `state` int(11) NOT NULL DEFAULT '0',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`patient_state_id`),
  UNIQUE KEY `patient_state_uuid_index` (`uuid`),
  KEY `patient_state_changer` (`changed_by`),
  KEY `patient_state_creator` (`creator`),
  KEY `patient_program_for_state` (`patient_program_id`),
  KEY `state_for_patient` (`state`),
  KEY `patient_state_voider` (`voided_by`),
  CONSTRAINT `patient_program_for_state` FOREIGN KEY (`patient_program_id`) REFERENCES `patient_program` (`patient_program_id`),
  CONSTRAINT `patient_state_changer` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `patient_state_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `patient_state_voider` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `state_for_patient` FOREIGN KEY (`state`) REFERENCES `program_workflow_state` (`program_workflow_state_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_state`
--

LOCK TABLES `patient_state` WRITE;
/*!40000 ALTER TABLE `patient_state` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `person`
--

DROP TABLE IF EXISTS `person`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `person` (
  `person_id` int(11) NOT NULL AUTO_INCREMENT,
  `gender` varchar(50) DEFAULT '',
  `birthdate` date DEFAULT NULL,
  `birthdate_estimated` tinyint(1) NOT NULL DEFAULT '0',
  `dead` tinyint(1) NOT NULL DEFAULT '0',
  `death_date` datetime DEFAULT NULL,
  `cause_of_death` int(11) DEFAULT NULL,
  `creator` int(11) DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`person_id`),
  UNIQUE KEY `person_uuid_index` (`uuid`),
  KEY `person_birthdate` (`birthdate`),
  KEY `person_death_date` (`death_date`),
  KEY `person_died_because` (`cause_of_death`),
  KEY `user_who_changed_person` (`changed_by`),
  KEY `user_who_created_person` (`creator`),
  KEY `user_who_voided_person` (`voided_by`),
  CONSTRAINT `person_died_because` FOREIGN KEY (`cause_of_death`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `user_who_changed_person` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_created_person` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_person` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `person`
--

LOCK TABLES `person` WRITE;
/*!40000 ALTER TABLE `person` DISABLE KEYS */;
INSERT INTO `person` VALUES (1,'M',NULL,0,0,NULL,NULL,NULL,'2005-01-01 00:00:00',NULL,NULL,0,NULL,NULL,NULL,'1296b0dc-440a-11e6-a65c-00e04c680037');
/*!40000 ALTER TABLE `person` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `person_address`
--

DROP TABLE IF EXISTS `person_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `person_address` (
  `person_address_id` int(11) NOT NULL AUTO_INCREMENT,
  `person_id` int(11) DEFAULT NULL,
  `preferred` tinyint(1) NOT NULL DEFAULT '0',
  `address1` varchar(255) DEFAULT NULL,
  `address2` varchar(255) DEFAULT NULL,
  `city_village` varchar(255) DEFAULT NULL,
  `state_province` varchar(255) DEFAULT NULL,
  `postal_code` varchar(50) DEFAULT NULL,
  `country` varchar(50) DEFAULT NULL,
  `latitude` varchar(50) DEFAULT NULL,
  `longitude` varchar(50) DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `county_district` varchar(255) DEFAULT NULL,
  `address3` varchar(255) DEFAULT NULL,
  `address4` varchar(255) DEFAULT NULL,
  `address5` varchar(255) DEFAULT NULL,
  `address6` varchar(255) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`person_address_id`),
  UNIQUE KEY `person_address_uuid_index` (`uuid`),
  KEY `patient_address_creator` (`creator`),
  KEY `address_for_person` (`person_id`),
  KEY `patient_address_void` (`voided_by`),
  KEY `person_address_changed_by` (`changed_by`),
  CONSTRAINT `address_for_person` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`) ON UPDATE CASCADE,
  CONSTRAINT `patient_address_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `patient_address_void` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `person_address_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `person_address`
--

LOCK TABLES `person_address` WRITE;
/*!40000 ALTER TABLE `person_address` DISABLE KEYS */;
/*!40000 ALTER TABLE `person_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `person_attribute`
--

DROP TABLE IF EXISTS `person_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `person_attribute` (
  `person_attribute_id` int(11) NOT NULL AUTO_INCREMENT,
  `person_id` int(11) NOT NULL DEFAULT '0',
  `value` varchar(50) NOT NULL DEFAULT '',
  `person_attribute_type_id` int(11) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`person_attribute_id`),
  UNIQUE KEY `person_attribute_uuid_index` (`uuid`),
  KEY `attribute_changer` (`changed_by`),
  KEY `attribute_creator` (`creator`),
  KEY `defines_attribute_type` (`person_attribute_type_id`),
  KEY `identifies_person` (`person_id`),
  KEY `attribute_voider` (`voided_by`),
  CONSTRAINT `attribute_changer` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `attribute_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `attribute_voider` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `defines_attribute_type` FOREIGN KEY (`person_attribute_type_id`) REFERENCES `person_attribute_type` (`person_attribute_type_id`),
  CONSTRAINT `identifies_person` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `person_attribute`
--

LOCK TABLES `person_attribute` WRITE;
/*!40000 ALTER TABLE `person_attribute` DISABLE KEYS */;
/*!40000 ALTER TABLE `person_attribute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `person_attribute_type`
--

DROP TABLE IF EXISTS `person_attribute_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `person_attribute_type` (
  `person_attribute_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL DEFAULT '',
  `description` text NOT NULL,
  `format` varchar(50) DEFAULT NULL,
  `foreign_key` int(11) DEFAULT NULL,
  `searchable` tinyint(1) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `edit_privilege` varchar(255) DEFAULT NULL,
  `sort_weight` double DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`person_attribute_type_id`),
  UNIQUE KEY `person_attribute_type_uuid_index` (`uuid`),
  KEY `attribute_is_searchable` (`searchable`),
  KEY `name_of_attribute` (`name`),
  KEY `person_attribute_type_retired_status` (`retired`),
  KEY `attribute_type_changer` (`changed_by`),
  KEY `attribute_type_creator` (`creator`),
  KEY `user_who_retired_person_attribute_type` (`retired_by`),
  KEY `privilege_which_can_edit` (`edit_privilege`),
  CONSTRAINT `attribute_type_changer` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `attribute_type_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `privilege_which_can_edit` FOREIGN KEY (`edit_privilege`) REFERENCES `privilege` (`privilege`),
  CONSTRAINT `user_who_retired_person_attribute_type` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `person_attribute_type`
--

LOCK TABLES `person_attribute_type` WRITE;
/*!40000 ALTER TABLE `person_attribute_type` DISABLE KEYS */;
INSERT INTO `person_attribute_type` VALUES (1,'Race','Group of persons related by common descent or heredity','java.lang.String',0,0,1,'2007-05-04 00:00:00',NULL,NULL,0,NULL,NULL,NULL,NULL,6,'8d871386-c2cc-11de-8d13-0010c6dffd0f'),(2,'Birthplace','Location of persons birth','java.lang.String',0,0,1,'2007-05-04 00:00:00',NULL,NULL,0,NULL,NULL,NULL,NULL,0,'8d8718c2-c2cc-11de-8d13-0010c6dffd0f'),(3,'Citizenship','Country of which this person is a member','java.lang.String',0,0,1,'2007-05-04 00:00:00',NULL,NULL,0,NULL,NULL,NULL,NULL,1,'8d871afc-c2cc-11de-8d13-0010c6dffd0f'),(4,'Mother\'s Name','First or last name of this person\'s mother','java.lang.String',0,0,1,'2007-05-04 00:00:00',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'8d871d18-c2cc-11de-8d13-0010c6dffd0f'),(5,'Civil Status','Marriage status of this person','org.openmrs.Concept',1054,0,1,'2007-05-04 00:00:00',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'8d871f2a-c2cc-11de-8d13-0010c6dffd0f'),(6,'Health District','District/region in which this patient\' home health center resides','java.lang.String',0,0,1,'2007-05-04 00:00:00',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'8d872150-c2cc-11de-8d13-0010c6dffd0f'),(7,'Health Center','Specific Location of this person\'s home health center.','org.openmrs.Location',0,0,1,'2007-05-04 00:00:00',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'8d87236c-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `person_attribute_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `person_merge_log`
--

DROP TABLE IF EXISTS `person_merge_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `person_merge_log` (
  `person_merge_log_id` int(11) NOT NULL AUTO_INCREMENT,
  `winner_person_id` int(11) NOT NULL,
  `loser_person_id` int(11) NOT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `merged_data` longtext NOT NULL,
  `uuid` char(38) NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`person_merge_log_id`),
  UNIQUE KEY `person_merge_log_unique_uuid` (`uuid`),
  KEY `person_merge_log_winner` (`winner_person_id`),
  KEY `person_merge_log_loser` (`loser_person_id`),
  KEY `person_merge_log_creator` (`creator`),
  KEY `person_merge_log_changed_by_fk` (`changed_by`),
  KEY `person_merge_log_voided_by_fk` (`voided_by`),
  CONSTRAINT `person_merge_log_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `person_merge_log_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `person_merge_log_loser` FOREIGN KEY (`loser_person_id`) REFERENCES `person` (`person_id`),
  CONSTRAINT `person_merge_log_voided_by_fk` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `person_merge_log_winner` FOREIGN KEY (`winner_person_id`) REFERENCES `person` (`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `person_merge_log`
--

LOCK TABLES `person_merge_log` WRITE;
/*!40000 ALTER TABLE `person_merge_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `person_merge_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `person_name`
--

DROP TABLE IF EXISTS `person_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `person_name` (
  `person_name_id` int(11) NOT NULL AUTO_INCREMENT,
  `preferred` tinyint(1) NOT NULL DEFAULT '0',
  `person_id` int(11) NOT NULL,
  `prefix` varchar(50) DEFAULT NULL,
  `given_name` varchar(50) DEFAULT NULL,
  `middle_name` varchar(50) DEFAULT NULL,
  `family_name_prefix` varchar(50) DEFAULT NULL,
  `family_name` varchar(50) DEFAULT NULL,
  `family_name2` varchar(50) DEFAULT NULL,
  `family_name_suffix` varchar(50) DEFAULT NULL,
  `degree` varchar(50) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`person_name_id`),
  UNIQUE KEY `person_name_uuid_index` (`uuid`),
  KEY `first_name` (`given_name`),
  KEY `last_name` (`family_name`),
  KEY `middle_name` (`middle_name`),
  KEY `family_name2` (`family_name2`),
  KEY `user_who_made_name` (`creator`),
  KEY `name_for_person` (`person_id`),
  KEY `user_who_voided_name` (`voided_by`),
  CONSTRAINT `name_for_person` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`) ON UPDATE CASCADE,
  CONSTRAINT `user_who_made_name` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_name` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `person_name`
--

LOCK TABLES `person_name` WRITE;
/*!40000 ALTER TABLE `person_name` DISABLE KEYS */;
INSERT INTO `person_name` VALUES (1,1,1,NULL,'Super','',NULL,'User',NULL,NULL,NULL,1,'2005-01-01 00:00:00',0,NULL,NULL,NULL,NULL,NULL,'1299c191-440a-11e6-a65c-00e04c680037');
/*!40000 ALTER TABLE `person_name` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `privilege`
--

DROP TABLE IF EXISTS `privilege`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `privilege` (
  `privilege` varchar(50) NOT NULL DEFAULT '',
  `description` varchar(250) NOT NULL DEFAULT '',
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`privilege`),
  UNIQUE KEY `privilege_uuid_index` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `privilege`
--

LOCK TABLES `privilege` WRITE;
/*!40000 ALTER TABLE `privilege` DISABLE KEYS */;
INSERT INTO `privilege` VALUES ('Add Allergies','Add allergies','ca605a31-de8a-4e68-b027-dad31f5334f0'),('Add Cohorts','Able to add a cohort to the system','129a991e-440a-11e6-a65c-00e04c680037'),('Add Concept Proposals','Able to add concept proposals to the system','129a9a2f-440a-11e6-a65c-00e04c680037'),('Add Encounters','Able to add patient encounters','129a9a7a-440a-11e6-a65c-00e04c680037'),('Add HL7 Inbound Archive','Able to add an HL7 archive item','d73a9b2a-eb12-41f3-9837-50e92d684631'),('Add HL7 Inbound Exception','Able to add an HL7 error item','9191cf01-dddd-49e9-a23c-1af42629fee8'),('Add HL7 Inbound Queue','Able to add an HL7 Queue item','0fd8e147-54d0-45cb-bd3c-39a57f951a69'),('Add HL7 Source','Able to add an HL7 Source','5953079c-09f1-4b7a-9315-c84b3448671d'),('Add Observations','Able to add patient observations','129a9abd-440a-11e6-a65c-00e04c680037'),('Add Orders','Able to add orders','129a9af6-440a-11e6-a65c-00e04c680037'),('Add Patient Identifiers','Able to add patient identifiers','129a9b30-440a-11e6-a65c-00e04c680037'),('Add Patient Programs','Able to add patients to programs','129a9b6f-440a-11e6-a65c-00e04c680037'),('Add Patients','Able to add patients','129a9bab-440a-11e6-a65c-00e04c680037'),('Add People','Able to add person objects','129a9be1-440a-11e6-a65c-00e04c680037'),('Add Problems','Add problems','88c4f7c1-e062-44eb-b2ec-1d01f5c4c922'),('Add Relationships','Able to add relationships','129a9c0a-440a-11e6-a65c-00e04c680037'),('Add Report Objects','Able to add report objects','129a9c32-440a-11e6-a65c-00e04c680037'),('Add Reports','Able to add reports','129a9c58-440a-11e6-a65c-00e04c680037'),('Add Users','Able to add users to OpenMRS','129a9c7c-440a-11e6-a65c-00e04c680037'),('Add Visits','Able to add visits','c6fe41a5-0daf-4047-b579-5c06c136bb8a'),('Assign System Developer Role','Able to assign System Developer role','df29939d-d3fd-47a5-b706-c1a3e1dea4da'),('Configure Visits','Able to choose encounter visit handler and enable/disable encounter visits','fa15cf48-ae39-49bd-b007-415e9607ec36'),('Delete Cohorts','Able to add a cohort to the system','129a9ca2-440a-11e6-a65c-00e04c680037'),('Delete Concept Proposals','Able to delete concept proposals from the system','129a9cc7-440a-11e6-a65c-00e04c680037'),('Delete Encounters','Able to delete patient encounters','129a9cf2-440a-11e6-a65c-00e04c680037'),('Delete HL7 Inbound Archive','Able to delete/retire an HL7 archive item','ef7e9169-405b-455b-8c70-f935b8e04e6b'),('Delete HL7 Inbound Exception','Able to delete an HL7 archive item','1c5a4e99-91da-4a94-ac45-3fb1857bbd66'),('Delete HL7 Inbound Queue','Able to delete an HL7 Queue item','e2b5ce59-9b8f-4ca7-9394-7f55b4fce59e'),('Delete Observations','Able to delete patient observations','129a9d18-440a-11e6-a65c-00e04c680037'),('Delete Orders','Able to delete orders','129a9d3c-440a-11e6-a65c-00e04c680037'),('Delete Patient Identifiers','Able to delete patient identifiers','129a9d62-440a-11e6-a65c-00e04c680037'),('Delete Patient Programs','Able to delete patients from programs','129a9d89-440a-11e6-a65c-00e04c680037'),('Delete Patients','Able to delete patients','129a9daf-440a-11e6-a65c-00e04c680037'),('Delete People','Able to delete objects','129a9dd5-440a-11e6-a65c-00e04c680037'),('Delete Relationships','Able to delete relationships','129a9dfa-440a-11e6-a65c-00e04c680037'),('Delete Report Objects','Able to delete report objects','129a9e21-440a-11e6-a65c-00e04c680037'),('Delete Reports','Able to delete reports','129a9e45-440a-11e6-a65c-00e04c680037'),('Delete Users','Able to delete users in OpenMRS','129a9e6a-440a-11e6-a65c-00e04c680037'),('Delete Visits','Able to delete visits','fdcc981a-5102-4294-b16b-4f9230d7aa17'),('Edit Allergies','Able to edit allergies','3dba9a6c-3ff3-46da-acfc-18a3d3e281a2'),('Edit Cohorts','Able to add a cohort to the system','129a9e8f-440a-11e6-a65c-00e04c680037'),('Edit Concept Proposals','Able to edit concept proposals in the system','129a9eb2-440a-11e6-a65c-00e04c680037'),('Edit Encounters','Able to edit patient encounters','129a9ed8-440a-11e6-a65c-00e04c680037'),('Edit Observations','Able to edit patient observations','129a9efb-440a-11e6-a65c-00e04c680037'),('Edit Orders','Able to edit orders','129a9f1f-440a-11e6-a65c-00e04c680037'),('Edit Patient Identifiers','Able to edit patient identifiers','129a9f43-440a-11e6-a65c-00e04c680037'),('Edit Patient Programs','Able to edit patients in programs','129a9f68-440a-11e6-a65c-00e04c680037'),('Edit Patients','Able to edit patients','129a9f8d-440a-11e6-a65c-00e04c680037'),('Edit People','Able to edit person objects','129a9fb2-440a-11e6-a65c-00e04c680037'),('Edit Problems','Able to edit problems','72725628-b493-4a50-8bd1-24dddfc2ea38'),('Edit Relationships','Able to edit relationships','129a9fd7-440a-11e6-a65c-00e04c680037'),('Edit Report Objects','Able to edit report objects','129a9ffa-440a-11e6-a65c-00e04c680037'),('Edit Reports','Able to edit reports','129aa01f-440a-11e6-a65c-00e04c680037'),('Edit User Passwords','Able to change the passwords of users in OpenMRS','129aa044-440a-11e6-a65c-00e04c680037'),('Edit Users','Able to edit users in OpenMRS','129aa06c-440a-11e6-a65c-00e04c680037'),('Edit Visits','Able to edit visits','31889d58-cd72-4865-ac4a-fc17abeb0385'),('Form Entry','Allows user to access Form Entry pages/functions','129aa08f-440a-11e6-a65c-00e04c680037'),('Manage Address Templates','Able to add/edit/delete address templates','02ddc03c-c8b3-4a23-a8b3-0ad1696ceaad'),('Manage Alerts','Able to add/edit/delete user alerts','129aa0ca-440a-11e6-a65c-00e04c680037'),('Manage Concept Classes','Able to add/edit/retire concept classes','129aa10e-440a-11e6-a65c-00e04c680037'),('Manage Concept Datatypes','Able to add/edit/retire concept datatypes','129aa19d-440a-11e6-a65c-00e04c680037'),('Manage Concept Map Types','Able to add/edit/retire concept map types','69cb3944-e328-4155-b34a-b4e90068d341'),('Manage Concept Name tags','Able to add/edit/delete concept name tags','58e22b94-4d63-4b37-b05e-763d87cba490'),('Manage Concept Reference Terms','Able to add/edit/retire reference terms','4215be25-34e7-4427-b85b-e68ea4aef1a7'),('Manage Concept Sources','Able to add/edit/delete concept sources','129aa1f0-440a-11e6-a65c-00e04c680037'),('Manage Concept Stop Words','Able to view/add/remove the concept stop words','96e0c2c6-a387-4ef4-b6a0-9a6ca8e850f2'),('Manage Concepts','Able to add/edit/delete concept entries','129aa22e-440a-11e6-a65c-00e04c680037'),('Manage Encounter Roles','Able to add/edit/retire encounter roles','61307a3e-1429-4ae6-a85e-65c92eac7a73'),('Manage Encounter Types','Able to add/edit/delete encounter types','129aa27e-440a-11e6-a65c-00e04c680037'),('Manage Field Types','Able to add/edit/retire field types','129aa2ae-440a-11e6-a65c-00e04c680037'),('Manage FormEntry XSN','Allows user to upload and edit the xsns stored on the server','129aa2d2-440a-11e6-a65c-00e04c680037'),('Manage Forms','Able to add/edit/delete forms','129aa2f8-440a-11e6-a65c-00e04c680037'),('Manage Global Properties','Able to add/edit global properties','129aa31d-440a-11e6-a65c-00e04c680037'),('Manage HL7 Messages','Able to add/edit/delete HL7 messages','a2d1789b-f032-4438-9db0-19af0fc57372'),('Manage Identifier Types','Able to add/edit/delete patient identifier types','129aa343-440a-11e6-a65c-00e04c680037'),('Manage Implementation Id','Able to view/add/edit the implementation id for the system','eb3c8909-1f36-45b2-b4fd-c452c74072a5'),('Manage Location Attribute Types','Able to add/edit/retire location attribute types','ed3c3f96-6118-4073-8adb-9ef1b7485967'),('Manage Location Tags','Able to add/edit/delete location tags','6bb71fbb-b1c6-4530-8637-67cfc65aa27e'),('Manage Locations','Able to add/edit/delete locations','129aa367-440a-11e6-a65c-00e04c680037'),('Manage Modules','Able to add/remove modules to the system','129aa38b-440a-11e6-a65c-00e04c680037'),('Manage Order Types','Able to add/edit/retire order types','129aa3b1-440a-11e6-a65c-00e04c680037'),('Manage Person Attribute Types','Able to add/edit/delete person attribute types','129aa3d4-440a-11e6-a65c-00e04c680037'),('Manage Privileges','Able to add/edit/delete privileges','129aa403-440a-11e6-a65c-00e04c680037'),('Manage Programs','Able to add/view/delete patient programs','129aa428-440a-11e6-a65c-00e04c680037'),('Manage Providers','Able to edit Provider','5652e96c-1718-4f95-8985-89a149e24d3d'),('Manage Relationship Types','Able to add/edit/retire relationship types','129aa44c-440a-11e6-a65c-00e04c680037'),('Manage Relationships','Able to add/edit/delete relationships','129aa474-440a-11e6-a65c-00e04c680037'),('Manage Roles','Able to add/edit/delete user roles','129aa499-440a-11e6-a65c-00e04c680037'),('Manage Rule Definitions','Allows creation and editing of user-defined rules','e6ae0588-b5cc-4293-b1db-fd93dc7195ee'),('Manage Scheduler','Able to add/edit/remove scheduled tasks','129aa4bd-440a-11e6-a65c-00e04c680037'),('Manage Tokens','Allows registering and removal of tokens','25e5779a-dd62-4e64-b958-e5f5b3ebfc8a'),('Manage Visit Attribute Types','Able to add/edit/retire visit attribute types','b62faa38-42f1-4aed-aa3a-207e4e67383c'),('Manage Visit Types','Able to add/edit/delete visit types','75ad24c1-a338-4978-8685-dfaf7257e1fc'),('Patient Dashboard - View Demographics Section','Able to view the \'Demographics\' tab on the patient dashboard','129aa4e2-440a-11e6-a65c-00e04c680037'),('Patient Dashboard - View Encounters Section','Able to view the \'Encounters\' tab on the patient dashboard','129aa50c-440a-11e6-a65c-00e04c680037'),('Patient Dashboard - View Forms Section','Allows user to view the Forms tab on the patient dashboard','129aa538-440a-11e6-a65c-00e04c680037'),('Patient Dashboard - View Graphs Section','Able to view the \'Graphs\' tab on the patient dashboard','129aa561-440a-11e6-a65c-00e04c680037'),('Patient Dashboard - View Overview Section','Able to view the \'Overview\' tab on the patient dashboard','129aa58b-440a-11e6-a65c-00e04c680037'),('Patient Dashboard - View Patient Summary','Able to view the \'Summary\' tab on the patient dashboard','129aa5b8-440a-11e6-a65c-00e04c680037'),('Patient Dashboard - View Regimen Section','Able to view the \'Regimen\' tab on the patient dashboard','129aa5e3-440a-11e6-a65c-00e04c680037'),('Patient Dashboard - View Visits Section','Able to view the \'Visits\' tab on the patient dashboard','811c6f3c-cd84-449a-b69e-231965988883'),('Purge Field Types','Able to purge field types','129aa612-440a-11e6-a65c-00e04c680037'),('Remove Allergies','Remove allergies','27cb9394-d387-4d7f-81e3-297debcd31f4'),('Remove Problems','Remove problems','b6f4d61f-fe31-48ac-9cb4-bae94f459053'),('Update HL7 Inbound Archive','Able to update an HL7 archive item','e85bf2ff-319c-41a7-833f-4a3a51eb9cdf'),('Update HL7 Inbound Exception','Able to update an HL7 archive item','1f1ca308-60f3-4533-834c-15ee6d9d59a2'),('Update HL7 Inbound Queue','Able to update an HL7 Queue item','5ec6d080-c576-48c2-b289-b46b1266d4ec'),('Update HL7 Source','Able to update an HL7 Source','674c571b-dc22-4950-a2ad-276f867ed9d9'),('Upload XSN','Allows user to upload/overwrite the XSNs defined for forms','129aa63c-440a-11e6-a65c-00e04c680037'),('View Administration Functions','Able to view the \'Administration\' link in the navigation bar','129aa661-440a-11e6-a65c-00e04c680037'),('View Allergies','Able to view allergies in OpenMRS','129aa68c-440a-11e6-a65c-00e04c680037'),('View Concept Classes','Able to view concept classes','129aa7b0-440a-11e6-a65c-00e04c680037'),('View Concept Datatypes','Able to view concept datatypes','129aa7dc-440a-11e6-a65c-00e04c680037'),('View Concept Map Types','Able to view concept map types','cb04f1fc-edde-42e3-896c-9d0aa77fab9c'),('View Concept Proposals','Able to view concept proposals to the system','129aa805-440a-11e6-a65c-00e04c680037'),('View Concept Reference Terms','Able to view concept reference terms','3c75e763-6a1b-4e2b-807e-fdb4da56ffb9'),('View Concept Sources','Able to view concept sources','129aa82c-440a-11e6-a65c-00e04c680037'),('View Concepts','Able to view concept entries','129aa850-440a-11e6-a65c-00e04c680037'),('View Data Entry Statistics','Able to view data entry statistics from the admin screen','129aa875-440a-11e6-a65c-00e04c680037'),('View Database Changes','Able to view database changes from the admin screen','1fa3f3a5-8352-49c9-a5e5-9c7775435fde'),('View Encounter Roles','Able to view encounter roles','404919ba-2342-406a-aa77-1a534fcb22c9'),('View Encounter Types','Able to view encounter types','129aa89c-440a-11e6-a65c-00e04c680037'),('View Encounters','Able to view patient encounters','129aa8c1-440a-11e6-a65c-00e04c680037'),('View Field Types','Able to view field types','129aa8e5-440a-11e6-a65c-00e04c680037'),('View Forms','Able to view forms','129aa90b-440a-11e6-a65c-00e04c680037'),('View Global Properties','Able to view global properties on the administration screen','129aa92f-440a-11e6-a65c-00e04c680037'),('View HL7 Inbound Archive','Able to view an HL7 archive item','9b73181b-921a-4e41-adeb-baa8c9cf91dd'),('View HL7 Inbound Exception','Able to view an HL7 archive item','bcaa6134-30d5-4faf-8aba-2faf522baf0e'),('View HL7 Inbound Queue','Able to view an HL7 Queue item','c8f22ace-31c3-422f-98fe-4dba4eb928d1'),('View HL7 Source','Able to view an HL7 Source','e17e9f4a-72ce-4a9f-8d13-4e2ce6b3b3af'),('View Identifier Types','Able to view patient identifier types','129aa954-440a-11e6-a65c-00e04c680037'),('View Location Attribute Types','Able to view location attribute types','a3df5c70-c951-4c23-a7c8-00167bb6bd1c'),('View Locations','Able to view locations','129aa979-440a-11e6-a65c-00e04c680037'),('View Navigation Menu','Ability to see the navigation menu','129aa99d-440a-11e6-a65c-00e04c680037'),('View Observations','Able to view patient observations','129aa9c2-440a-11e6-a65c-00e04c680037'),('View Order Types','Able to view order types','129aa9e7-440a-11e6-a65c-00e04c680037'),('View Orders','Able to view orders','129aaa0a-440a-11e6-a65c-00e04c680037'),('View Patient Cohorts','Able to view patient cohorts','129aaa2e-440a-11e6-a65c-00e04c680037'),('View Patient Identifiers','Able to view patient identifiers','129aaa54-440a-11e6-a65c-00e04c680037'),('View Patient Programs','Able to see which programs that patients are in','129aaa7b-440a-11e6-a65c-00e04c680037'),('View Patients','Able to view patients','129aaaa0-440a-11e6-a65c-00e04c680037'),('View People','Able to view person objects','129aaac5-440a-11e6-a65c-00e04c680037'),('View Person Attribute Types','Able to view person attribute types','129aaaeb-440a-11e6-a65c-00e04c680037'),('View Privileges','Able to view user privileges','129aab11-440a-11e6-a65c-00e04c680037'),('View Problems','Able to view problems in OpenMRS','129aab35-440a-11e6-a65c-00e04c680037'),('View Programs','Able to view patient programs','129aab59-440a-11e6-a65c-00e04c680037'),('View Providers','Able to view Provider','8202e173-7d10-4da9-b279-424f056ee7ec'),('View Relationship Types','Able to view relationship types','129aab7d-440a-11e6-a65c-00e04c680037'),('View Relationships','Able to view relationships','129aaba4-440a-11e6-a65c-00e04c680037'),('View Report Objects','Able to view report objects','129aabc8-440a-11e6-a65c-00e04c680037'),('View Reports','Able to view reports','129aabee-440a-11e6-a65c-00e04c680037'),('View Roles','Able to view user roles','129aac14-440a-11e6-a65c-00e04c680037'),('View Rule Definitions','Allows viewing of user-defined rules. (This privilege is not necessary to run rules under normal usage.)','ee1809f0-b147-4947-8793-2c324ce9b1e0'),('View Unpublished Forms','Able to view and fill out unpublished forms','129aac3a-440a-11e6-a65c-00e04c680037'),('View Users','Able to view users in OpenMRS','129aac63-440a-11e6-a65c-00e04c680037'),('View Visit Attribute Types','Able to view visit attribute types','6ed39527-c5e0-4267-afb2-263dcd2c300b'),('View Visit Types','Able to view visit types','f82294e8-57d5-4f05-9b3e-05671ad5da20'),('View Visits','Able to view visits','0b67ced8-a59c-4476-af36-25498b096c8f');
/*!40000 ALTER TABLE `privilege` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `program`
--

DROP TABLE IF EXISTS `program`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `program` (
  `program_id` int(11) NOT NULL AUTO_INCREMENT,
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `outcomes_concept_id` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `name` varchar(50) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`program_id`),
  UNIQUE KEY `program_uuid_index` (`uuid`),
  KEY `user_who_changed_program` (`changed_by`),
  KEY `program_concept` (`concept_id`),
  KEY `program_creator` (`creator`),
  KEY `program_outcomes_concept_id_fk` (`outcomes_concept_id`),
  CONSTRAINT `program_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `program_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `program_outcomes_concept_id_fk` FOREIGN KEY (`outcomes_concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `user_who_changed_program` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `program`
--

LOCK TABLES `program` WRITE;
/*!40000 ALTER TABLE `program` DISABLE KEYS */;
/*!40000 ALTER TABLE `program` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `program_workflow`
--

DROP TABLE IF EXISTS `program_workflow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `program_workflow` (
  `program_workflow_id` int(11) NOT NULL AUTO_INCREMENT,
  `program_id` int(11) NOT NULL DEFAULT '0',
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`program_workflow_id`),
  UNIQUE KEY `program_workflow_uuid_index` (`uuid`),
  KEY `workflow_changed_by` (`changed_by`),
  KEY `workflow_concept` (`concept_id`),
  KEY `workflow_creator` (`creator`),
  KEY `program_for_workflow` (`program_id`),
  CONSTRAINT `program_for_workflow` FOREIGN KEY (`program_id`) REFERENCES `program` (`program_id`),
  CONSTRAINT `workflow_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `workflow_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `workflow_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `program_workflow`
--

LOCK TABLES `program_workflow` WRITE;
/*!40000 ALTER TABLE `program_workflow` DISABLE KEYS */;
/*!40000 ALTER TABLE `program_workflow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `program_workflow_state`
--

DROP TABLE IF EXISTS `program_workflow_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `program_workflow_state` (
  `program_workflow_state_id` int(11) NOT NULL AUTO_INCREMENT,
  `program_workflow_id` int(11) NOT NULL DEFAULT '0',
  `concept_id` int(11) NOT NULL DEFAULT '0',
  `initial` tinyint(1) NOT NULL DEFAULT '0',
  `terminal` tinyint(1) NOT NULL DEFAULT '0',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`program_workflow_state_id`),
  UNIQUE KEY `program_workflow_state_uuid_index` (`uuid`),
  KEY `state_changed_by` (`changed_by`),
  KEY `state_concept` (`concept_id`),
  KEY `state_creator` (`creator`),
  KEY `workflow_for_state` (`program_workflow_id`),
  CONSTRAINT `state_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `state_concept` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `state_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `workflow_for_state` FOREIGN KEY (`program_workflow_id`) REFERENCES `program_workflow` (`program_workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `program_workflow_state`
--

LOCK TABLES `program_workflow_state` WRITE;
/*!40000 ALTER TABLE `program_workflow_state` DISABLE KEYS */;
/*!40000 ALTER TABLE `program_workflow_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `provider`
--

DROP TABLE IF EXISTS `provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provider` (
  `provider_id` int(11) NOT NULL AUTO_INCREMENT,
  `person_id` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `identifier` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`provider_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `provider_changed_by_fk` (`changed_by`),
  KEY `provider_person_id_fk` (`person_id`),
  KEY `provider_retired_by_fk` (`retired_by`),
  KEY `provider_creator_fk` (`creator`),
  CONSTRAINT `provider_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `provider_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `provider_person_id_fk` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`),
  CONSTRAINT `provider_retired_by_fk` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `provider`
--

LOCK TABLES `provider` WRITE;
/*!40000 ALTER TABLE `provider` DISABLE KEYS */;
/*!40000 ALTER TABLE `provider` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `provider_attribute`
--

DROP TABLE IF EXISTS `provider_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provider_attribute` (
  `provider_attribute_id` int(11) NOT NULL AUTO_INCREMENT,
  `provider_id` int(11) NOT NULL,
  `attribute_type_id` int(11) NOT NULL,
  `value_reference` text NOT NULL,
  `uuid` char(38) NOT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`provider_attribute_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `provider_attribute_provider_fk` (`provider_id`),
  KEY `provider_attribute_attribute_type_id_fk` (`attribute_type_id`),
  KEY `provider_attribute_creator_fk` (`creator`),
  KEY `provider_attribute_changed_by_fk` (`changed_by`),
  KEY `provider_attribute_voided_by_fk` (`voided_by`),
  CONSTRAINT `provider_attribute_attribute_type_id_fk` FOREIGN KEY (`attribute_type_id`) REFERENCES `provider_attribute_type` (`provider_attribute_type_id`),
  CONSTRAINT `provider_attribute_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `provider_attribute_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `provider_attribute_provider_fk` FOREIGN KEY (`provider_id`) REFERENCES `provider` (`provider_id`),
  CONSTRAINT `provider_attribute_voided_by_fk` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `provider_attribute`
--

LOCK TABLES `provider_attribute` WRITE;
/*!40000 ALTER TABLE `provider_attribute` DISABLE KEYS */;
/*!40000 ALTER TABLE `provider_attribute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `provider_attribute_type`
--

DROP TABLE IF EXISTS `provider_attribute_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provider_attribute_type` (
  `provider_attribute_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `datatype` varchar(255) DEFAULT NULL,
  `datatype_config` text,
  `preferred_handler` varchar(255) DEFAULT NULL,
  `handler_config` text,
  `min_occurs` int(11) NOT NULL,
  `max_occurs` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`provider_attribute_type_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `provider_attribute_type_creator_fk` (`creator`),
  KEY `provider_attribute_type_changed_by_fk` (`changed_by`),
  KEY `provider_attribute_type_retired_by_fk` (`retired_by`),
  CONSTRAINT `provider_attribute_type_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `provider_attribute_type_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `provider_attribute_type_retired_by_fk` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `provider_attribute_type`
--

LOCK TABLES `provider_attribute_type` WRITE;
/*!40000 ALTER TABLE `provider_attribute_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `provider_attribute_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `relationship`
--

DROP TABLE IF EXISTS `relationship`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `relationship` (
  `relationship_id` int(11) NOT NULL AUTO_INCREMENT,
  `person_a` int(11) NOT NULL,
  `relationship` int(11) NOT NULL DEFAULT '0',
  `person_b` int(11) NOT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `date_changed` datetime DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`relationship_id`),
  UNIQUE KEY `relationship_uuid_index` (`uuid`),
  KEY `relation_creator` (`creator`),
  KEY `person_a_is_person` (`person_a`),
  KEY `person_b_is_person` (`person_b`),
  KEY `relationship_type_id` (`relationship`),
  KEY `relation_voider` (`voided_by`),
  KEY `relationship_changed_by` (`changed_by`),
  CONSTRAINT `person_a_is_person` FOREIGN KEY (`person_a`) REFERENCES `person` (`person_id`),
  CONSTRAINT `person_b_is_person` FOREIGN KEY (`person_b`) REFERENCES `person` (`person_id`),
  CONSTRAINT `relation_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `relation_voider` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `relationship_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `relationship_type_id` FOREIGN KEY (`relationship`) REFERENCES `relationship_type` (`relationship_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `relationship`
--

LOCK TABLES `relationship` WRITE;
/*!40000 ALTER TABLE `relationship` DISABLE KEYS */;
/*!40000 ALTER TABLE `relationship` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `relationship_type`
--

DROP TABLE IF EXISTS `relationship_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `relationship_type` (
  `relationship_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `a_is_to_b` varchar(50) NOT NULL,
  `b_is_to_a` varchar(50) NOT NULL,
  `preferred` tinyint(1) NOT NULL DEFAULT '0',
  `weight` int(11) NOT NULL DEFAULT '0',
  `description` varchar(255) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`relationship_type_id`),
  UNIQUE KEY `relationship_type_uuid_index` (`uuid`),
  KEY `user_who_created_rel` (`creator`),
  KEY `user_who_retired_relationship_type` (`retired_by`),
  CONSTRAINT `user_who_created_rel` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_relationship_type` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `relationship_type`
--

LOCK TABLES `relationship_type` WRITE;
/*!40000 ALTER TABLE `relationship_type` DISABLE KEYS */;
INSERT INTO `relationship_type` VALUES (1,'Doctor','Patient',0,0,'Relationship from a primary care provider to the patient',1,'2007-05-04 00:00:00',0,NULL,NULL,NULL,'8d919b58-c2cc-11de-8d13-0010c6dffd0f'),(2,'Sibling','Sibling',0,0,'Relationship between brother/sister, brother/brother, and sister/sister',1,'2007-05-04 00:00:00',0,NULL,NULL,NULL,'8d91a01c-c2cc-11de-8d13-0010c6dffd0f'),(3,'Parent','Child',0,0,'Relationship from a mother/father to the child',1,'2007-05-04 00:00:00',0,NULL,NULL,NULL,'8d91a210-c2cc-11de-8d13-0010c6dffd0f'),(4,'Aunt/Uncle','Niece/Nephew',0,0,'Relationship from a parent\'s sibling to a child of that parent',1,'2007-05-04 00:00:00',0,NULL,NULL,NULL,'8d91a3dc-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `relationship_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_object`
--

DROP TABLE IF EXISTS `report_object`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_object` (
  `report_object_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `report_object_type` varchar(255) NOT NULL,
  `report_object_sub_type` varchar(255) NOT NULL,
  `xml_data` text,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`report_object_id`),
  UNIQUE KEY `report_object_uuid_index` (`uuid`),
  KEY `user_who_changed_report_object` (`changed_by`),
  KEY `report_object_creator` (`creator`),
  KEY `user_who_voided_report_object` (`voided_by`),
  CONSTRAINT `report_object_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_changed_report_object` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_voided_report_object` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_object`
--

LOCK TABLES `report_object` WRITE;
/*!40000 ALTER TABLE `report_object` DISABLE KEYS */;
/*!40000 ALTER TABLE `report_object` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_schema_xml`
--

DROP TABLE IF EXISTS `report_schema_xml`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_schema_xml` (
  `report_schema_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `xml_data` text NOT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`report_schema_id`),
  UNIQUE KEY `report_schema_xml_uuid_index` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_schema_xml`
--

LOCK TABLES `report_schema_xml` WRITE;
/*!40000 ALTER TABLE `report_schema_xml` DISABLE KEYS */;
/*!40000 ALTER TABLE `report_schema_xml` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role` (
  `role` varchar(50) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`role`),
  UNIQUE KEY `role_uuid_index` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES ('Anonymous','Privileges for non-authenticated users.','774b2af3-6437-4e5a-a310-547554c7c65c'),('Authenticated','Privileges gained once authentication has been established.','f7fd42ef-880e-40c5-972d-e4ae7c990de2'),('Provider','All users with the \'Provider\' role will appear as options in the default Infopath ','8d94f280-c2cc-11de-8d13-0010c6dffd0f'),('System Developer','Developers of the OpenMRS .. have additional access to change fundamental structure of the database model.','8d94f852-c2cc-11de-8d13-0010c6dffd0f');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_privilege`
--

DROP TABLE IF EXISTS `role_privilege`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role_privilege` (
  `role` varchar(50) NOT NULL DEFAULT '',
  `privilege` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`privilege`,`role`),
  KEY `role_privilege_to_role` (`role`),
  CONSTRAINT `privilege_definitons` FOREIGN KEY (`privilege`) REFERENCES `privilege` (`privilege`),
  CONSTRAINT `role_privilege_to_role` FOREIGN KEY (`role`) REFERENCES `role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_privilege`
--

LOCK TABLES `role_privilege` WRITE;
/*!40000 ALTER TABLE `role_privilege` DISABLE KEYS */;
INSERT INTO `role_privilege` VALUES ('Authenticated','View Concept Classes'),('Authenticated','View Concept Datatypes'),('Authenticated','View Encounter Types'),('Authenticated','View Field Types'),('Authenticated','View Global Properties'),('Authenticated','View Identifier Types'),('Authenticated','View Locations'),('Authenticated','View Order Types'),('Authenticated','View Person Attribute Types'),('Authenticated','View Privileges'),('Authenticated','View Relationship Types'),('Authenticated','View Relationships'),('Authenticated','View Roles');
/*!40000 ALTER TABLE `role_privilege` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_role`
--

DROP TABLE IF EXISTS `role_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role_role` (
  `parent_role` varchar(50) NOT NULL DEFAULT '',
  `child_role` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`parent_role`,`child_role`),
  KEY `inherited_role` (`child_role`),
  CONSTRAINT `inherited_role` FOREIGN KEY (`child_role`) REFERENCES `role` (`role`),
  CONSTRAINT `parent_role` FOREIGN KEY (`parent_role`) REFERENCES `role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_role`
--

LOCK TABLES `role_role` WRITE;
/*!40000 ALTER TABLE `role_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `role_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scheduler_task_config`
--

DROP TABLE IF EXISTS `scheduler_task_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scheduler_task_config` (
  `task_config_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `schedulable_class` text,
  `start_time` datetime DEFAULT NULL,
  `start_time_pattern` varchar(50) DEFAULT NULL,
  `repeat_interval` int(11) NOT NULL DEFAULT '0',
  `start_on_startup` tinyint(1) NOT NULL DEFAULT '0',
  `started` tinyint(1) NOT NULL DEFAULT '0',
  `created_by` int(11) DEFAULT '0',
  `date_created` datetime DEFAULT '2005-01-01 00:00:00',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `last_execution_time` datetime DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`task_config_id`),
  UNIQUE KEY `scheduler_task_config_uuid_index` (`uuid`),
  KEY `scheduler_changer` (`changed_by`),
  KEY `scheduler_creator` (`created_by`),
  CONSTRAINT `scheduler_changer` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `scheduler_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scheduler_task_config`
--

LOCK TABLES `scheduler_task_config` WRITE;
/*!40000 ALTER TABLE `scheduler_task_config` DISABLE KEYS */;
INSERT INTO `scheduler_task_config` VALUES (1,'Update Concept Index','Iterates through the concept dictionary, re-creating the concept index (which are used for searcing). This task is started when using the \'Update Concept Index Storage\' page and no range is given.  This task stops itself when one iteration has completed.','org.openmrs.scheduler.tasks.ConceptIndexUpdateTask',NULL,NULL,0,0,0,1,'2005-01-01 00:00:00',2,'2016-07-07 08:15:10',NULL,'7c75911e-0310-11e0-8222-18a905e044dc'),(2,'Auto Close Visits Task','Stops all active visits that match the visit type(s) specified by the value of the global property \'visits.autoCloseVisitType\'','org.openmrs.scheduler.tasks.AutoCloseVisitsTask','2011-11-28 23:59:59','MM/dd/yyyy HH:mm:ss',86400,0,0,1,'2016-07-07 08:14:55',NULL,NULL,NULL,'8c17b376-1a2b-11e1-a51a-00248140a5eb'),(3,'Initialize Logic Rule Providers',NULL,'org.openmrs.logic.task.InitializeLogicRuleProvidersTask','2016-07-07 08:15:37',NULL,1999999999,0,1,2,'2016-07-07 08:15:07',2,'2016-07-07 08:15:38','2016-07-07 08:15:38','eb9e447d-5ef9-4771-9e88-21916340b69c');
/*!40000 ALTER TABLE `scheduler_task_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scheduler_task_config_property`
--

DROP TABLE IF EXISTS `scheduler_task_config_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scheduler_task_config_property` (
  `task_config_property_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `value` text,
  `task_config_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`task_config_property_id`),
  KEY `task_config_for_property` (`task_config_id`),
  CONSTRAINT `task_config_for_property` FOREIGN KEY (`task_config_id`) REFERENCES `scheduler_task_config` (`task_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scheduler_task_config_property`
--

LOCK TABLES `scheduler_task_config_property` WRITE;
/*!40000 ALTER TABLE `scheduler_task_config_property` DISABLE KEYS */;
/*!40000 ALTER TABLE `scheduler_task_config_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `serialized_object`
--

DROP TABLE IF EXISTS `serialized_object`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `serialized_object` (
  `serialized_object_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(5000) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `subtype` varchar(255) NOT NULL,
  `serialization_class` varchar(255) NOT NULL,
  `serialized_data` mediumtext NOT NULL,
  `date_created` datetime NOT NULL,
  `creator` int(11) NOT NULL,
  `date_changed` datetime DEFAULT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `date_retired` datetime DEFAULT NULL,
  `retired_by` int(11) DEFAULT NULL,
  `retire_reason` varchar(1000) DEFAULT NULL,
  `uuid` char(38) DEFAULT NULL,
  PRIMARY KEY (`serialized_object_id`),
  UNIQUE KEY `serialized_object_uuid_index` (`uuid`),
  KEY `serialized_object_creator` (`creator`),
  KEY `serialized_object_changed_by` (`changed_by`),
  KEY `serialized_object_retired_by` (`retired_by`),
  CONSTRAINT `serialized_object_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `serialized_object_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `serialized_object_retired_by` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `serialized_object`
--

LOCK TABLES `serialized_object` WRITE;
/*!40000 ALTER TABLE `serialized_object` DISABLE KEYS */;
/*!40000 ALTER TABLE `serialized_object` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_order`
--

DROP TABLE IF EXISTS `test_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `test_order` (
  `order_id` int(11) NOT NULL DEFAULT '0',
  `specimen_source` int(11) DEFAULT NULL,
  `laterality` varchar(20) DEFAULT NULL,
  `clinical_history` text,
  PRIMARY KEY (`order_id`),
  KEY `test_order_specimen_source_fk` (`specimen_source`),
  CONSTRAINT `test_order_order_id_fk` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `test_order_specimen_source_fk` FOREIGN KEY (`specimen_source`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_order`
--

LOCK TABLES `test_order` WRITE;
/*!40000 ALTER TABLE `test_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `test_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_property`
--

DROP TABLE IF EXISTS `user_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_property` (
  `user_id` int(11) NOT NULL DEFAULT '0',
  `property` varchar(100) NOT NULL DEFAULT '',
  `property_value` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`user_id`,`property`),
  CONSTRAINT `user_property_to_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_property`
--

LOCK TABLES `user_property` WRITE;
/*!40000 ALTER TABLE `user_property` DISABLE KEYS */;
INSERT INTO `user_property` VALUES (1,'lockoutTimestamp',''),(1,'loginAttempts','0');
/*!40000 ALTER TABLE `user_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_role` (
  `user_id` int(11) NOT NULL DEFAULT '0',
  `role` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`role`,`user_id`),
  KEY `user_role_to_users` (`user_id`),
  CONSTRAINT `role_definitions` FOREIGN KEY (`role`) REFERENCES `role` (`role`),
  CONSTRAINT `user_role_to_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_role`
--

LOCK TABLES `user_role` WRITE;
/*!40000 ALTER TABLE `user_role` DISABLE KEYS */;
INSERT INTO `user_role` VALUES (1,'Provider'),(1,'System Developer');
/*!40000 ALTER TABLE `user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `system_id` varchar(50) NOT NULL DEFAULT '',
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(128) DEFAULT NULL,
  `salt` varchar(128) DEFAULT NULL,
  `secret_question` varchar(255) DEFAULT NULL,
  `secret_answer` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `person_id` int(11) NOT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`user_id`),
  KEY `user_who_changed_user` (`changed_by`),
  KEY `user_creator` (`creator`),
  KEY `user_who_retired_this_user` (`retired_by`),
  KEY `person_id_for_user` (`person_id`),
  CONSTRAINT `person_id_for_user` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`),
  CONSTRAINT `user_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_changed_user` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `user_who_retired_this_user` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','admin','ffba0fa0e7638c1ed511d497cd24afc6a5bcf5231eb9661a87fe215201ba4ede020152a656f02f7ffa5ba34360fe4b0d1e237fe0efed8509fb087a1fadae437d','0ce631e430099bfb05c83cbac134f4ace05a178f642b252517bc60be24eb4c3d4240b0f4421915c894eb9443e7e66f64dc89f5ab47139c1201fb9015df17ec45',NULL,NULL,1,'2005-01-01 00:00:00',1,'2016-07-07 08:15:10',1,0,NULL,NULL,NULL,'1c3db49d-440a-11e6-a65c-00e04c680037'),(2,'daemon','daemon',NULL,NULL,NULL,NULL,1,'2010-04-26 13:25:00',NULL,NULL,1,0,NULL,NULL,NULL,'A4F30A1B-5EB9-11DF-A648-37A07F9C90FB');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visit`
--

DROP TABLE IF EXISTS `visit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `visit` (
  `visit_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_id` int(11) NOT NULL,
  `visit_type_id` int(11) NOT NULL,
  `date_started` datetime NOT NULL,
  `date_stopped` datetime DEFAULT NULL,
  `indication_concept_id` int(11) DEFAULT NULL,
  `location_id` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`visit_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `visit_patient_index` (`patient_id`),
  KEY `visit_type_fk` (`visit_type_id`),
  KEY `visit_location_fk` (`location_id`),
  KEY `visit_creator_fk` (`creator`),
  KEY `visit_voided_by_fk` (`voided_by`),
  KEY `visit_changed_by_fk` (`changed_by`),
  KEY `visit_indication_concept_fk` (`indication_concept_id`),
  CONSTRAINT `visit_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_indication_concept_fk` FOREIGN KEY (`indication_concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `visit_location_fk` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
  CONSTRAINT `visit_patient_fk` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`),
  CONSTRAINT `visit_type_fk` FOREIGN KEY (`visit_type_id`) REFERENCES `visit_type` (`visit_type_id`),
  CONSTRAINT `visit_voided_by_fk` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visit`
--

LOCK TABLES `visit` WRITE;
/*!40000 ALTER TABLE `visit` DISABLE KEYS */;
/*!40000 ALTER TABLE `visit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visit_attribute`
--

DROP TABLE IF EXISTS `visit_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `visit_attribute` (
  `visit_attribute_id` int(11) NOT NULL AUTO_INCREMENT,
  `visit_id` int(11) NOT NULL,
  `attribute_type_id` int(11) NOT NULL,
  `value_reference` text NOT NULL,
  `uuid` char(38) NOT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `voided` tinyint(1) NOT NULL DEFAULT '0',
  `voided_by` int(11) DEFAULT NULL,
  `date_voided` datetime DEFAULT NULL,
  `void_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`visit_attribute_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `visit_attribute_visit_fk` (`visit_id`),
  KEY `visit_attribute_attribute_type_id_fk` (`attribute_type_id`),
  KEY `visit_attribute_creator_fk` (`creator`),
  KEY `visit_attribute_changed_by_fk` (`changed_by`),
  KEY `visit_attribute_voided_by_fk` (`voided_by`),
  CONSTRAINT `visit_attribute_attribute_type_id_fk` FOREIGN KEY (`attribute_type_id`) REFERENCES `visit_attribute_type` (`visit_attribute_type_id`),
  CONSTRAINT `visit_attribute_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_attribute_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_attribute_visit_fk` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`visit_id`),
  CONSTRAINT `visit_attribute_voided_by_fk` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visit_attribute`
--

LOCK TABLES `visit_attribute` WRITE;
/*!40000 ALTER TABLE `visit_attribute` DISABLE KEYS */;
/*!40000 ALTER TABLE `visit_attribute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visit_attribute_type`
--

DROP TABLE IF EXISTS `visit_attribute_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `visit_attribute_type` (
  `visit_attribute_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `datatype` varchar(255) DEFAULT NULL,
  `datatype_config` text,
  `preferred_handler` varchar(255) DEFAULT NULL,
  `handler_config` text,
  `min_occurs` int(11) NOT NULL,
  `max_occurs` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`visit_attribute_type_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `visit_attribute_type_creator_fk` (`creator`),
  KEY `visit_attribute_type_changed_by_fk` (`changed_by`),
  KEY `visit_attribute_type_retired_by_fk` (`retired_by`),
  CONSTRAINT `visit_attribute_type_changed_by_fk` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_attribute_type_creator_fk` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_attribute_type_retired_by_fk` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visit_attribute_type`
--

LOCK TABLES `visit_attribute_type` WRITE;
/*!40000 ALTER TABLE `visit_attribute_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `visit_attribute_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visit_type`
--

DROP TABLE IF EXISTS `visit_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `visit_type` (
  `visit_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `creator` int(11) NOT NULL,
  `date_created` datetime NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `retired_by` int(11) DEFAULT NULL,
  `date_retired` datetime DEFAULT NULL,
  `retire_reason` varchar(255) DEFAULT NULL,
  `uuid` char(38) NOT NULL,
  PRIMARY KEY (`visit_type_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `visit_type_creator` (`creator`),
  KEY `visit_type_changed_by` (`changed_by`),
  KEY `visit_type_retired_by` (`retired_by`),
  CONSTRAINT `visit_type_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_type_creator` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `visit_type_retired_by` FOREIGN KEY (`retired_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visit_type`
--

LOCK TABLES `visit_type` WRITE;
/*!40000 ALTER TABLE `visit_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `visit_type` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-07-07  8:36:45
