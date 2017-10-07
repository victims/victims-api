/*
* victi.ms API microservice
* Copyright (C) 2017 The victi.ms team
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published
* by the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package db

import (
	"errors"
	"os"
	"time"

	"github.com/victims/victims-api/log"

	mgo "gopkg.in/mgo.v2"
)

// session is the Mongo session
var session *mgo.Session

// db is the package internal database connection
var db *mgo.Database

func New(mongoURI, mongoDB string) (*mgo.Database, error) {
	log.Logger.Infof("Creating new session %s/%s: ", mongoURI, mongoDB)
	ses, err := mgo.DialWithTimeout(mongoURI, 3*time.Second)
	if err != nil {
		log.Logger.Errorf("Could not connect to MongoDB: %s\n", err)
		return nil, err
	}
	session := *ses
	newdb := session.DB(mongoDB)
	log.Logger.Infof("Connection made %s\n", newdb.Name)
	if err != nil {
		log.Logger.Fatal(err)
	}
	db = newdb
	return db, nil
}

// GetDB returns a database connection.
func GetDB() (*mgo.Database, error) {
	if db == nil {
		return nil, errors.New(
			"Database not initialized. New() must be called once.")
	}
	return db, nil
}

// GetCollection returns a collection.
func GetCollection(collection string) (*mgo.Collection, error) {
	database, err := GetDB()
	if err != nil {
		return nil, err
	}
	foundCollection := database.C(collection)
	log.Logger.Debugf("Using collection %s", foundCollection.Name)
	return foundCollection, nil
}

// CloseSession closes the current session and resets unepxported variables
func CloseSession() error {
	log.Logger.Debug("Closing MongoDB session.")
	if session != nil {
		session.Close()
		db = nil
		session = nil
		log.Logger.Debug("MongoDB session closed and db/session set to nil")
	}
	log.Logger.Info("MongoDB session closed.")
	return nil
}

// lookupKeyOrFail looks up an environment variable. If it doesn't exist
// then painc, otherwise return the value of the key.
func lookupKeyOrFail(key string) string {
	value, ok := os.LookupEnv(key)
	if !ok {
		log.Logger.Panicf("The environment variable %s must be set!", key)
	}
	return value
}

// GetMongoDBAuthFromEnv returns the values of MONGODB_USER,
// MONGODB_PASSWORD, and MONGODB_DATABASE from the environment
func GetMongoDBAuthFromEnv() (string, string, string) {
	user := lookupKeyOrFail("MONGODB_USER")
	password := lookupKeyOrFail("MONGODB_PASSWORD")
	database := lookupKeyOrFail("MONGODB_DATABASE")

	return user, password, database
}
