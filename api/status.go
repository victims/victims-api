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
	"github.com/emicklei/go-restful"
)

// StatusResource represents the support status of this API
type StatusResource struct{}

// getStatus returns the support status of this API
func (s *StatusResource) getStatus(r *restful.Request, w *restful.Response) {
	w.WriteEntity(Status{
		Eol:         nil,
		Supported:   true,
		Version:     Version,
		Recommended: true,
		Endpoint:    Endpoint,
	})

}

// Register mounts the resource into a restful container
func (s *StatusResource) Register(container *restful.Container) {
	ws := new(restful.WebService)
	ws.
		Path(Endpoint).
		Doc("Manage Users").
		Consumes(restful.MIME_JSON).
		Produces(restful.MIME_JSON)

	ws.Route(ws.GET("/status.json").To(s.getStatus).
		Doc("get a the api support status").
		Operation("getStatus"))

	container.Add(ws)
}
