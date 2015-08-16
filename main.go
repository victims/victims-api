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
package main

import (
	"github.com/ashcrow/victims-api/api"
	"github.com/ashcrow/victims-api/conf"
	"github.com/emicklei/go-restful"
	//"github.com/emicklei/go-restful/swagger"
	flag "github.com/ogier/pflag"
	goserv "gopkg.in/ashcrow/go-serv.v0"
)

// main takes care of how to run when called via the CLI
func main() {
	// Add MongoDB flags to the parser
	flag.StringVar(&conf.Config.MongoURI, "MongoURI", conf.Config.MongoURI, "MongoDB server URI")
	flag.StringVar(&conf.Config.MongoDatabase, "MongoDatabase", conf.Config.MongoDatabase, "MongoDB database")

	// Make a new server.
	server := goserv.NewServer(&conf.Config.BaseConfiguration)
	wsContainer := restful.NewContainer()
	p := api.Product{}
	p.Register(wsContainer)

	status := api.StatusResource{}
	status.Register(wsContainer)

	groups := api.GroupsResource{}
	groups.Register(wsContainer)

	hash := api.HashResource{}
	hash.Register(wsContainer)

	wsContainer.ServiceErrorHandler(api.ErrorHandler)

	// Enable swagger
	/*config := swagger.Config{
		WebServices: wsContainer.RegisteredWebServices(),
		ApiPath:     "/service/v3/apidocs.json",
	}
	swagger.RegisterSwaggerService(config, wsContainer)
	*/
	// Set the handler to our gorilla mux router wrapped with LogAccess
	server.Handler = goserv.LogAccess(wsContainer)
	goserv.Logger.Fatal(server.ListenAndServe())
}
