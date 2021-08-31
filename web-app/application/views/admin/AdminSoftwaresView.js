/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var AdminSoftwaresView = Backbone.View.extend({

    render: function () {
        var self = this;
        if (!this.rendered) {
            require(["text!application/templates/admin/AdminSoftwares.tpl.html"],
                function (tpl) {
                    self.getValues(function() {
                        self.doLayout(tpl);
                        self.rendered = true;
                        window.setSoftwareAdminTabInstance();
                    });
                }
            );
        }
    },

    doLayout: function(tpl) {
        var self = this;


        var view = _.template(tpl, {});
        $(this.el).append(view);


        return this;
    },

    getValues: function(callback) {
            callback();
    }

});

