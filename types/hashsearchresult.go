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

// HashSearchResult holds a list of hashs found during a search of the databse.
type HashSearchResult struct {
	Hashes []Hash `bson:"hashes" json:"hashes"`
}

// AddHash appens a hash to the Hashes attribute.
func (hsr *HashSearchResult) AddHash(h Hash) {
	hsr.Hashes = append(hsr.Hashes, h)
}

// NewHashSearchResult creates a new HashSearchResult instance.
func NewHashSearchResult() *HashSearchResult {
	hashes := make([]Hash, 0)
	hsr := HashSearchResult{
		Hashes: hashes,
	}
	return &hsr
}
