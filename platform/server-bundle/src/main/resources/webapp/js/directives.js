(function () {
    'use strict';

    var widgetModule = angular.module('motech-widgets', []);

    widgetModule.directive('layout', function() {
        return {
            link: function(scope, elm, attrs) {
                var northSelector, southSelector, westSelector, westSouthSelector, $Container;
                /*
                * Define options for outer layout
                */
                scope.outerLayoutOptions = {
                    name: 'outerLayout',
                    //resizeWithWindowDelay: 250, // delay calling resizeAll when window is *still* resizing
                    // resizeWithWindowMaxDelay: 2000, // force resize every XX ms while window is being resized
                    resizable: true,  // sizable: false,
                    slidable: true,
                    closable: true,
                    north__closable: false,
                    north__paneSelector: "#outer-north",
                    west__paneSelector: "#outer-west",
                    center__paneSelector: "#outer-center",
                    south__paneSelector: "#outer-south",
                    south__spacing_open: 0,
                    south__spacing_closed: 0,
                    south__minSize: 30,
                    south__size: 30,
                    north__spacing_open: 0,
                    west__spacing_open: 6,
                    spacing_closed: 30,
                    north__spacing_closed: 0,
                    north__minSize: 40,
                    //north__showOverflowOnHover: true,
                    west__size: 350,
                    useStateCookie: true,
                    cookie__keys: "north.size,north.isClosed,south.size,south.isClosed,west.size,west.isClosed,east.size,east.isClosed",
                    showErrorMessages: true, // some panes do not have an inner layout
                    resizeWhileDragging: true,
                    center__minHeight: 100,
                    contentSelector: ".ui-layout-content",
                    togglerContent_closed: '<span><i class="icon-caret-right button"></i></span>',
                    togglerContent_open: '<span><i class="icon-caret-left button"></i></span>',
                    togglerAlign_closed: "top", // align to top of resizer
                    togglerAlign_open: "top",
                    togglerLength_open: 0, // NONE - using custom togglers INSIDE west-pane
                    togglerLength_closed: 35,
                    togglerTip_open: "Close This Pane",
                    togglerTip_closed: "Open This Pane",
                    slideTrigger_open: "click",  // default
                    initClosed: false,
                    south__initClosed: true
                };

                scope.outerLayout = elm.layout( scope.outerLayoutOptions);
                scope.outerLayout.sizePane('north', scope.showDashboardLogo.changeHeight());

                // BIND events to hard-coded buttons in the NORTH toolbar
                scope.outerLayout.addOpenBtn( "#tbarOpenSouth2", "south" );
                scope.outerLayout.addCloseBtn( "#tbarCloseSouth", "south" );
                scope.outerLayout.addCloseBtn( "#tbarCloseWest", "west" );

            }
        };
    });

    widgetModule.directive('innerlayout', function() {
        return {
            link: function(scope, elm, attrs) {
                var eastSelector;
                /*
                * Define options for inner layout
                */
                scope.innerLayoutOptions = {
                    name: 'innerLayout',
                    resizeWithWindowDelay: 250, // delay calling resizeAll when window is *still* resizing
                    // resizeWithWindowMaxDelay: 2000, // force resize every XX ms while window is being resized
                    resizable: true,
                    slidable: true,
                    closable: true,
                    east__paneSelector: "#inner-east",
                    center__paneSelector: "#inner-center",
                    east__spacing_open: 6,
                    spacing_closed: 30,
                    //east__showOverflowOnHover: true,
                    east__size: 300,
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
                    east__initClosed: false,
                    initHidden: false
                    //isHidden: true
                };

                // create the page-layout, which will ALSO create the tabs-wrapper child-layout
                scope.innerLayout = elm.layout(scope.innerLayoutOptions);

                // BIND events to hard-coded buttons in the NORTH toolbar
                scope.innerLayout.addCloseBtn( "#tbarCloseEast", "east" );
            }
        };
    });

    widgetModule.directive('elementactive', function() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                $(element).find('li').livequery(function () {
                    $(this).on({
                         click: function (e) {
                             $(element).children().removeClass('active');
                             $(this).addClass('active');
                             $(element).children().children().attr("href");
                             $('#side-nav').children().addClass('hidden');
                             //$($(this).children().attr("href")).removeClass('hidden');
                             $($(this).children().attr("href").replace('#', ".")).removeClass('hidden');
                         }
                    });

                });
            }
        };
    });

    widgetModule.directive('overflowChangePanel', function () {
            return {
                restrict: 'A',
                link: function (scope, element, attrs) {
                    $(element).find('.overflowChange').livequery(function () {
                        $(this).on({
                            mouseover: function (e) {
                                if (!e.target.classList.contains("overflowChange")) {
                                    $(element).parent().parent().css('overflow', 'visible');
                                    $(element).parent().parent().css('z-index', '10');
                                }
                            },
                            mouseout: function (e) {
                                if (!$(this).hasClass("open")) {
                                    $(element).parent().parent().css('overflow', 'hidden');
                                    $(element).parent().parent().css('z-index', '0');
                                }
                            }
                        });
                    });
                }
            };
        });

    widgetModule.directive('bsPopover', function() {
        return function(scope, element, attrs) {
            $(element).popover({
                content: function () {
                    return attrs.bsPopover;
                }
            });
        };
    });

    widgetModule.directive('elementFocus', function() {
        return function (scope, element, atts) {
            element[0].focus();
        };
    });

    widgetModule.directive('goToTop', function () {
        return function (scope, element, attrs) {
            $(element).click(function () {
                $('body, html').animate({
                    scrollTop: 0
                }, 800);

                return false;
            });
        };
    });

    widgetModule.directive('goToEnd', function () {
        return function (scope, element, attrs) {
            $(element).click(function () {
                $('body, html').animate({
                    scrollTop: $(document).height()
                }, 800);

                return false;
            });
        };
    });

    widgetModule.directive('ngDateTimePicker', function () {
        return function (scope, element, attributes) {
            $(element).datetimepicker();
        };
    });

    widgetModule.directive('serverTime', function () {
        return function (scope, element, attributes) {
            var getTime = function() {
                $.post('gettime', function(time) {
                     $(element).text(time);
                });
            };

            getTime();
            window.setInterval(getTime, 60000);
        };
    });

    widgetModule.directive('serverUpTime', function () {
        return function (scope, element, attributes) {
            var getUptime = function() {
                $.post('getUptime', function(time) {
                     $(element).text(moment(time).fromNow());
                });
            };

            getUptime();
            window.setInterval(getUptime, 60000);
        };
    });

    widgetModule.directive('motechModules', function ($compile, $timeout, $http, $templateCache) {
        var templateLoader;

        return {
            restrict: 'E',
            replace : true,
            transclude: true,
            compile: function (tElement, tAttrs, scope) {
                var url = '../server/resources/partials/motech-modules.html',

                templateLoader = $http.get(url, {cache: $templateCache})
                    .success(function (html) {
                        tElement.html(html);
                    });

                return function (scope, element, attrs) {
                    templateLoader.then(function () {
                        element.html($compile(tElement.html())(scope));
                    });
                };
            }
        };
    });

}());
