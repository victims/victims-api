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

import (
	"github.com/ashcrow/victims-api/db"
	"github.com/emicklei/go-restful"
	"labix.org/v2/mgo/bson"
)

// StatusResource represents the support status of this API
type HashResource struct{}

// getStatus returns the support status of this API
func (h *HashResource) getHash(r *restful.Request, w *restful.Response) {
	hash := r.PathParameter("hash")
	database, _ := db.GetDB()
	col := database.C("hashes")
	cursor := col.Find(bson.M{"hashes.sha512.combined": hash})
	count, _ := cursor.Count()
	if count == 1 {
		result := db.Hash{}
		cursor.One(&result)
		w.WriteEntity(result)
	} else {
		w.WriteEntity(APIError{Error: "Object not found."})
	}
}

// Register mounts the resource into a restful container
func (h *HashResource) Register(container *restful.Container) {
	ws := new(restful.WebService)
	ws.
		Path(Endpoint + "hash/").
		Doc("Manage Users").
		Consumes(restful.MIME_JSON).
		Produces(restful.MIME_JSON)

	ws.Route(ws.GET("{hash}/").To(h.getHash).
		Doc("get a hash").
		Operation("getHash"))

	container.Add(ws)
}
