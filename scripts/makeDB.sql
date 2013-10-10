-- MySQL dump 10.13  Distrib 5.5.31, for debian-linux-gnu (i686)
--
-- Host: localhost    Database: stockdata_test
-- ------------------------------------------------------
-- Server version	5.5.31-0+wheezy1

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
-- Table structure for table `DailyNet`
--

DROP TABLE IF EXISTS `DailyNet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DailyNet` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  `price` decimal(8,3) DEFAULT NULL,
  `net` decimal(8,3) DEFAULT NULL,
  `totalVolume` bigint(20) DEFAULT NULL,
  `net1` decimal(8,3) DEFAULT NULL,
  `net2` decimal(8,3) DEFAULT NULL,
  `net3` decimal(8,3) DEFAULT NULL,
  `net4` decimal(8,3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EndOfDayQuotes`
--

DROP TABLE IF EXISTS `EndOfDayQuotes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EndOfDayQuotes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbolid` bigint(20) DEFAULT NULL,
  `symbol` varchar(10) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `open` decimal(8,3) DEFAULT NULL,
  `close` decimal(8,3) DEFAULT NULL,
  `high` decimal(8,3) DEFAULT NULL,
  `low` decimal(8,3) DEFAULT NULL,
  `price` decimal(8,3) DEFAULT NULL,
  `volume` bigint(20) DEFAULT NULL,
  `chng` decimal(8,3) DEFAULT NULL,
  `chgper` decimal(7,3) DEFAULT NULL,
  `bid` decimal(8,3) DEFAULT NULL,
  `ask` decimal(8,3) DEFAULT NULL,
  `bidsize` bigint(20) DEFAULT NULL,
  `asksize` bigint(20) DEFAULT NULL,
  `yearlo` decimal(8,3) DEFAULT NULL,
  `yearhi` decimal(8,3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=52124 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Holdings`
--

DROP TABLE IF EXISTS `Holdings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Holdings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbolid` bigint(20) DEFAULT NULL,
  `symbol` varchar(20) DEFAULT NULL,
  `buy_date` datetime DEFAULT NULL,
  `buy_price` decimal(8,3) DEFAULT NULL,
  `buy_volume` bigint(20) DEFAULT NULL,
  `buy_total` decimal(8,3) DEFAULT NULL,
  `criteria` varchar(200) DEFAULT NULL,
  `sell_date` datetime DEFAULT NULL,
  `sell_price` decimal(8,3) DEFAULT NULL,
  `sell_volume` bigint(20) DEFAULT NULL,
  `sell_total` decimal(8,3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1157 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Holdings1`
--

DROP TABLE IF EXISTS `Holdings1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Holdings1` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbolid` bigint(20) DEFAULT NULL,
  `symbol` varchar(20) DEFAULT NULL,
  `buy_date` datetime DEFAULT NULL,
  `buy_price` decimal(8,3) DEFAULT NULL,
  `buy_volume` bigint(20) DEFAULT NULL,
  `buy_total` decimal(8,3) DEFAULT NULL,
  `criteria` varchar(200) DEFAULT NULL,
  `sell_date` datetime DEFAULT NULL,
  `sell_price` decimal(8,3) DEFAULT NULL,
  `sell_volume` bigint(20) DEFAULT NULL,
  `sell_total` decimal(8,3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Holdings2`
--

DROP TABLE IF EXISTS `Holdings2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Holdings2` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbolid` bigint(20) DEFAULT NULL,
  `symbol` varchar(20) DEFAULT NULL,
  `buy_date` datetime DEFAULT NULL,
  `buy_price` decimal(8,3) DEFAULT NULL,
  `buy_volume` bigint(20) DEFAULT NULL,
  `buy_total` decimal(8,3) DEFAULT NULL,
  `criteria` varchar(200) DEFAULT NULL,
  `sell_date` datetime DEFAULT NULL,
  `sell_price` decimal(8,3) DEFAULT NULL,
  `sell_volume` bigint(20) DEFAULT NULL,
  `sell_total` decimal(8,3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Holdings3`
--

DROP TABLE IF EXISTS `Holdings3`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Holdings3` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbolid` bigint(20) DEFAULT NULL,
  `symbol` varchar(20) DEFAULT NULL,
  `buy_date` datetime DEFAULT NULL,
  `buy_price` decimal(8,3) DEFAULT NULL,
  `buy_volume` bigint(20) DEFAULT NULL,
  `buy_total` decimal(8,3) DEFAULT NULL,
  `criteria` varchar(200) DEFAULT NULL,
  `sell_date` datetime DEFAULT NULL,
  `sell_price` decimal(8,3) DEFAULT NULL,
  `sell_volume` bigint(20) DEFAULT NULL,
  `sell_total` decimal(8,3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Holdings4`
--

DROP TABLE IF EXISTS `Holdings4`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Holdings4` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbolid` bigint(20) DEFAULT NULL,
  `symbol` varchar(20) DEFAULT NULL,
  `buy_date` datetime DEFAULT NULL,
  `buy_price` decimal(8,3) DEFAULT NULL,
  `buy_volume` bigint(20) DEFAULT NULL,
  `buy_total` decimal(8,3) DEFAULT NULL,
  `criteria` varchar(200) DEFAULT NULL,
  `sell_date` datetime DEFAULT NULL,
  `sell_price` decimal(8,3) DEFAULT NULL,
  `sell_volume` bigint(20) DEFAULT NULL,
  `sell_total` decimal(8,3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RealTimeQuotes`
--

DROP TABLE IF EXISTS `RealTimeQuotes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RealTimeQuotes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` datetime DEFAULT NULL,
  `symbol` varchar(20) DEFAULT NULL,
  `price` decimal(8,3) DEFAULT NULL,
  `volume` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `symbol` (`symbol`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=7643 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `calendar`
--

DROP TABLE IF EXISTS `calendar`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `calendar` (
  `date` date NOT NULL,
  `market_is_open` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `symbols`
--

DROP TABLE IF EXISTS `symbols`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `symbols` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbol` varchar(6) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `market_cap` double DEFAULT NULL,
  `sector` varchar(50) DEFAULT NULL,
  `industry` varchar(75) DEFAULT NULL,
  `exchange` varchar(10) DEFAULT NULL,
  `expired` int(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=14651 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-10-06  9:08:14
