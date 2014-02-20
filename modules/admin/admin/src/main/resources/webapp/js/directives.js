(function () {
    'use strict';

    /* Directives */

    var widgetModule = angular.module('motech-admin');

    widgetModule.directive('innerlayout', function() {
         return {
             link: function(scope, elm, attrs) {
                 var eastSelector;
                 /*
                 * Define options for inner layout
                 */
                 scope.innerLayoutOptions = {
                     name: 'innerLayout',
                         resizable: true,
                         slidable: true,
                         closable: true,
                         east__paneSelector: "#inner-east",
                         center__paneSelector: "#inner-center",
                         east__spacing_open: 6,
                         spacing_closed: 30,
                         east__size: 300,
                         east__minSize: 200,
                         east__maxSize: 350,
                         center__size: 100,
                         showErrorMessages: true, // some panes do not have an inner layout
                         resizeWhileDragging: true,
                         center__minHeight: 100,
                         contentSelector: ".ui-layout-content",
                         togglerContent_open: '',
                         togglerContent_closed: '<div><i class="icon-caret-left button"></i></div>',
                         autoReopen: false, // auto-open panes that were previously auto-closed due to 'no room'
                         noRoom: true,
                         togglerAlign_closed: "top", // align to top of resizer
                         togglerAlign_open: "top",
                         togglerLength_open: 0,
                         togglerLength_closed: 35,
                         togglerTip_open: "Close This Pane",
                         togglerTip_closed: "Open This Pane",
                         east__initClosed: true,
                         initHidden: true
                 };

                 // create the page-layout, which will ALSO create the tabs-wrapper child-layout
                 scope.innerLayout = elm.layout(scope.innerLayoutOptions);

             }
         };
    });

    widgetModule.directive('sidebar', function () {
       return function (scope, element, attrs) {
           $(element).sidebar({
               position:"right"
           });
       };
    });

    widgetModule.directive('clearForm', function () {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                $(element).on('hidden', function () {
                    $('#' + attrs.clearForm).clearForm();
                });
            }
        };
    });

    widgetModule.directive('gridDatePickerFrom', function() {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var elem = angular.element(element),
                    endDateTextBox = angular.element('#dateTimeTo');

                elem.datetimepicker({
                    dateFormat: "yy-mm-dd",
                    changeMonth: true,
                    changeYear: true,
                    timeFormat: "HH:mm:ss",
                    onSelect: function (selectedDateTime){
                        endDateTextBox.datetimepicker('option', 'minDate', elem.datetimepicker('getDate') );
                    }
                });
            }
        };
    });

    widgetModule.directive('gridDatePickerTo', function() {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var elem = angular.element(element),
                    startDateTextBox = angular.element('#dateTimeFrom');

                elem.datetimepicker({
                    dateFormat: "yy-mm-dd",
                    changeMonth: true,
                    changeYear: true,
                    timeFormat: "HH:mm:ss",
                    onSelect: function (selectedDateTime){
                        startDateTextBox.datetimepicker('option', 'maxDate', elem.datetimepicker('getDate') );
                    }
                });
            }
        };
    });

}());