angular.module('cramsite').directive('ngTarget', function ($parse, $timeout) {
    var NON_ASSIGNABLE_MODEL_EXPRESSION = 'Non-assignable model expression: ';
    return {
        restrict: "A",
        link: function (scope, element, attr) {
            var buildGetterSetter = function (name) {
                var me = {};
                me.get = $parse(name);
                me.set = me.get.assign;
                if (!me.set) {
                    throw Error(NON_ASSIGNABLE_MODEL_EXPRESSION + name);
                }
                return me;
            };

            // *********** focus *********** 
            var focusTriggerName = attr.ngTarget + "._focusTrigger";
            var focusTrigger = buildGetterSetter(focusTriggerName);
            var focus = buildGetterSetter(attr.ngTarget + ".focus");

            focusTrigger.set(scope, 0);
            focus.set(scope, function () {
                focusTrigger.set(scope, 1);
            });

            // $watch the trigger variable for a transition
            scope.$watch(focusTriggerName, function (newValue, oldValue) {
                if (newValue > 0) {
                    $timeout(function () { // a timing workaround hack
                        element[0].focus(); // without jQuery, need [0]
                        focusTrigger.set(scope, 0);
                    }, 50);
                }
            });

            // *********** blur *********** 
            var blurTriggerName = attr.ngTarget + "._blurTrigger";
            var blurTrigger = buildGetterSetter(blurTriggerName);
            var blur = buildGetterSetter(attr.ngTarget + ".blur");

            blurTrigger.set(scope, 0);
            blur.set(scope, function () {
                blurTrigger.set(scope, 1);
            });

            // $watch the trigger variable for a transition
            scope.$watch(blurTriggerName, function (newValue, oldValue) {
                if (newValue > 0) {
                    $timeout(function () { // a timing workaround hack
                        element[0].blur(); // without jQuery, need [0]
                        blurTrigger.set(scope, 0);
                    }, 50);
                }
            });

            // *********** select *********** 
            var selectTriggerName = attr.ngTarget + "._selectTrigger";
            var selectTrigger = buildGetterSetter(selectTriggerName);
            var select = buildGetterSetter(attr.ngTarget + ".select");

            selectTrigger.set(scope, 0);
            select.set(scope, function () {
                selectTrigger.set(scope, 1);
            });

            // $watch the trigger variable for a transition
            scope.$watch(selectTriggerName, function (newValue, oldValue) {
                if (newValue > 0) {
                    $timeout(function () { // a timing workaround hack
                        element[0].select(); // without jQuery, need [0]
                        selectTrigger.set(scope, 0);
                    }, 50);
                }
            });

        }
    };
});