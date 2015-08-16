/*
* victi.ms API microservice
* Copyright (C) 2015 The victi.ms team
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
	"time"

	"github.com/ashcrow/victims-api/conf"
	goserv "gopkg.in/ashcrow/go-serv.v0"
	"labix.org/v2/mgo" // MongoDB driver
)

// session is the Mongo session
var session *mgo.Session

// db is the package internal database connection
var db *mgo.Database

// GetDB returns a database connection.
func GetDB() (*mgo.Database, error) {
	if db == nil && session == nil {
		goserv.Logger.Info("Creating new session: ", conf.Config.MongoURI)
		ses, err := mgo.DialWithTimeout(conf.Config.MongoURI, 3*time.Second)
		if err != nil {
			goserv.Logger.Error("Could not connect to MongoDB: ", err)
			return nil, err
		}
		session := *ses
		newdb := session.DB(conf.Config.MongoDatabase)
		goserv.Logger.Infof("Connection made %s", newdb.Name)
		if err != nil {
			goserv.Logger.Fatal(err)
		}
		db = newdb
	}
	return db, nil
}

// CloseSession closes the current session and resets unepxported variables
func CloseSession() error {
	goserv.Logger.Debug("Closing MongoDB session.")
	if session != nil {
		session.Close()
		db = nil
		session = nil
		goserv.Logger.Debug("MongoDB session closed and db/session set to nil")
	}
	goserv.Logger.Info("MongoDB session closed.")
	return nil
}
