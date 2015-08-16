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
package api

const (
	Version  = 3
	Endpoint = "/service/v3/"
)

func isValidAlgorithm(i string) bool {
	for _, v := range []string{"sha512", "sha1", "md5"} {
		if i == v {
			return true
		}
	}
	return false
}

func isHash(i string) bool {
	for _, ok := range []int{256, 512} {
		if len(i) == ok {
			return true
		}
	}
	return false
}
