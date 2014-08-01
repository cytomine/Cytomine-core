/**
 * Created by lrollus on 22/04/14.
 */
angular.module("cytomineUserArea")
    .directive('userTable', function () {
        return {
            restrict:'E',
            templateUrl: "components/userTable/userTable.html",
            scope: {
                rolesFn: "&roles",
                userFn: "&user"
            },
            controller : function($scope) {
                $scope.getLabel = function(item) {
                    var label = "label-info";
                    if(item.authority=="ROLE_SUPER_ADMIN") {
                        label = "label-default";
                    } else if(item.authority=="ROLE_ADMIN") {
                        label = "label-danger";
                    } else if(item.authority=="ROLE_USER") {
                        label = "label-success";
                    } else if(item.authority=="ROLE_GUEST") {
                        label = "label-primary";
                    }
                    return label;
                };

                $scope.roleSorter = function(item) {
                    var index = 4;
                    if(item.authority=="ROLE_SUPER_ADMIN") {
                        index = 0;
                    } else if(item.authority=="ROLE_ADMIN") {
                        index = 1;
                    } else if(item.authority=="ROLE_USER") {
                        index = 2;
                    } else if(item.authority=="ROLE_GUEST") {
                        index = 3;
                    }
                    return index
                }
            }
        };
    })
    .directive('userJson', function () {
        return {
            restrict:'E',
            templateUrl: "components/userTable/userJson.html",
            scope: {
                userFn: "&user"
            }
        };
    })
;