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
)

// Group is an an abstraction for dealing with listing supported Groups
type GroupsResource struct{}

// getGroups gets all Groups
func (g *GroupsResource) getGroups(r *restful.Request, w *restful.Response) {
	database, _ := db.GetDB()
	col := database.C("hashes")
	result := []string{}
	col.Find(nil).Distinct("group", &result)
	w.WriteEntity(result)
}

// Register mounts the resource into a restful container
func (g *GroupsResource) Register(container *restful.Container) {
	ws := new(restful.WebService)
	ws.
		Path(Endpoint + "groups/").
		Doc("Manage Users").
		Consumes(restful.MIME_JSON).
		Produces(restful.MIME_JSON)

	ws.Route(ws.GET("").To(g.getGroups).
		Doc("get all groups").
		Operation("getGroups").
		Writes([]string{}))

	container.Add(ws)
}
