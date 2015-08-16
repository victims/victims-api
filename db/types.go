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
	"labix.org/v2/mgo/bson" // MongoDB driver
	"time"
)

// Hash is a representation of the Hash instances stored in MongoDB
type Hash struct {
	ID          bson.ObjectId                     `bson:"_id,omitempty"`
	V1          map[string]int                    `bson:"_v1" json:"_v1"`
	Date        time.Time                         `bson:"date" json:"date"`
	CreatedOn   time.Time                         `bson:"createdon" json:"createdon"`
	Hash        string                            `bson:"hash" json:"hash"`
	Name        string                            `bson:"name" json:"name"`
	Version     string                            `bson:"version" json:"version"`
	Coordinates map[string]string                 `bson:"coordinates" json:"coordinates"`
	Group       string                            `bson:"group" json:"group"`
	Format      string                            `bson:"format" json:"format"`
	Hashes      map[string]map[string]interface{} `bson:"hashes" json:"hashes"`
	Vendor      string                            `bson:"vendor" json:"vendor"`
	Cves        []map[string]interface{}          `bson:"cves" json:"cves"`
	Status      string                            `bson:"status" json:"status"`
	Metadata    []map[string]interface{}          `bson:"metadata" json:"metadata"`
	Submitter   string                            `bson:"submitter" json:"submitter"`
	Submittedon time.Time                         `bson:"submittedon" json:"submittedon"`
}
