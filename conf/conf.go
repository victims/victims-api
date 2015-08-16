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
package conf

import (
	"time"

	goserv "gopkg.in/ashcrow/go-serv.v0"
)

// VictimsConfiguration is the configuration struct used throughout the microservice
type VictimsConfiguration struct {
	goserv.BaseConfiguration
	MongoDatabase string
	MongoURI      string
}

// Config is our configuration
var Config *VictimsConfiguration

func init() {
	// Set defaults for the server configuration with BaseConfiguration
	// inside of VictimsConfiguration.
	Config = &VictimsConfiguration{
		BaseConfiguration: goserv.BaseConfiguration{
			BindAddress:    "127.0.0.1",
			BindPort:       8080,
			ReadTimeout:    10 * time.Second,
			WriteTimeout:   10 * time.Second,
			MaxHeaderBytes: 1 << 20,
			LogLevel:       "info",
		},
		MongoURI:      "mongodb://127.0.0.1/",
		MongoDatabase: "victims",
	}
}
