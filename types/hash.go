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

package types

import (
	"time"

	"gopkg.in/mgo.v2/bson"
)

// Hash is a representation of a single hash stored in the databse.
type Hash struct {
	ID           bson.ObjectId            `bson:"_id,omitempty" description:"Internally used ID for the hash"`
	Date         time.Time                `bson:"date" json:"date" description:"Updated date of artifact"`
	CreatedOn    time.Time                `bson:"createdon" json:"createdon" description:"Date the hash was created"`
	Hash         string                   `bson:"hash" json:"hash" description:"The hash string itself"`
	CombinedHash string                   `bson:"combinedhash" json:"combinedhash" description:"Hash of all the files"`
	FileHashes   map[string]interface{}   `bson:"filehashes" json:"filehashes" description:"File hashes for the artifact"`
	Name         string                   `bson:"name" json:"name" description:"Name of the artifact"`
	Version      string                   `bson:"version" json:"version" description:"Version of the artifact"`
	Group        string                   `bson:"group" json:"group" description:"Type of artifact"`
	Format       string                   `bson:"format" json:"format" description:"Format of the artifact"`
	Vendor       string                   `bson:"vendor" json:"vendor" description:"Vendor of the artifact"`
	Cves         CVEs                     `bson:"cves" json:"cves" description:"All known related CVEs"`
	Status       string                   `bson:"status" json:"status" description:"Status of the hash in the database"`
	Metadata     []map[string]interface{} `bson:"metadata" json:"metadata" description:"Misc metadata"`
	Submitter    string                   `bson:"submitter" json:"submitter" description:"User who submitted the hash"`
}

// SingleHashRequest represents a single hash request via HTTP
type SingleHashRequest struct {
	Hash string `binding:"required" json:"hash" description:"The hash being requested"`
}

// MultipleHashRequest represents multiple hashes being requested over HTTP
type MultipleHashRequest struct {
	Hashes []SingleHashRequest `binding:"required" json:"hashes" description:"All hashes being requested"`
}
