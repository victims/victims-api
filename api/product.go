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

// Product is an an abstraction for dealing with Hashes from a product POV
type Product struct{}

// getProduct gets Hashes for a single product
func (h *Product) getProduct(r *restful.Request, w *restful.Response) {
	name := r.PathParameter("name")
	database, _ := db.GetDB()
	col := database.C("hashes")
	cursor := col.Find(bson.M{"name": name})
	count, _ := cursor.Count()
	if count >= 1 {
		result := []db.Hash{}
		cursor.All(&result)
		w.WriteEntity(result)
	} else {
		w.WriteEntity(APIError{Error: "Object not found."})
	}
}

// getProductByVersion gets Hashes for a specific product version
func (h *Product) getProductByVersion(r *restful.Request, w *restful.Response) {
	name := r.PathParameter("name")
	version := r.PathParameter("version")

	database, _ := db.GetDB()
	col := database.C("hashes")
	cursor := col.Find(bson.M{"name": name, "version": version})
	count, _ := cursor.Count()
	if count >= 1 {
		result := []db.Hash{}
		cursor.All(&result)
		w.WriteEntity(result)
	} else {
		w.WriteEntity(APIError{Error: "Object not found."})
	}
}

/* TODO
// getProductByVersionRange gets Hashes for a specific product version range
func (h *Product) getProductByVersion(r *restful.Request, w *restful.Response) {
	name := r.PathParameter("name")
	startVersion := r.PathParameter("startVersion")
	endVersion := r.PathParameter("endVersion")

	database, _ := db.GetDB()
	col := database.C("hashes")
	cursor := col.Find(bson.M{"name": name, "version": version})
	count, _ := cursor.Count()
	if count >= 1 {
		result := []db.Hash{}
		cursor.All(&result)
		w.WriteEntity(result)
	} else {
		w.WriteEntity(APIError{Error: "Object not found."})
	}
}*/

// Register mounts the resource into a restful container
func (h *Product) Register(container *restful.Container) {
	ws := new(restful.WebService)
	ws.
		Path(Endpoint + "product/").
		Doc("Manage Users").
		Consumes(restful.MIME_JSON).
		Produces(restful.MIME_JSON)

	ws.Route(ws.GET("{name}/").To(h.getProduct).
		Doc("get a product").
		Operation("getProduct").
		Param(ws.PathParameter("name", "identifier of the product").DataType("string")).
		Writes([]db.Hash{}))

	ws.Route(ws.GET("{name}/{version}/").To(h.getProductByVersion).
		Doc("get a product by it's name and version").
		Operation("getProductByVersion").
		Param(ws.PathParameter("name", "identifier of the product").DataType("string")).
		Param(ws.PathParameter("version", "version of the product").DataType("string")).
		Writes([]db.Hash{}))

	container.Add(ws)
}
