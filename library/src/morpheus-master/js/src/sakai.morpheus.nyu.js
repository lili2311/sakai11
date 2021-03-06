/* CLASSES-2264 CLASSES-1319 NYU When neo templates are used,
   ensure the content pane matches the tool menu height
*/
$(function() {
  var $toolMenu = $("#toolMenu");
  var $portlet = $("#content");

  if ($portlet.length == 1) {
    var menuHeight = $toolMenu.height();

    // CLASSES-2316 We want to ensure the footer is planted at the bottom
    // of the windows so ensure the min-height of the portlet panel is enough
    // to do this
    if ($(document.body).height() < $(window).height()) {
      var footerHeight = $("#footer").outerHeight();
      var headerHeight = $portlet.offset().top;
      var calculatedHeight = $(window).height() - headerHeight -  footerHeight;

      menuHeight = Math.max(menuHeight, calculatedHeight);
    }

    $portlet.css("minHeight", menuHeight + "px");
  }
});


// Introduce Option button for calendar and messages synoptic tool
// that invokes option link within the tool
$(function() {
  $("#synopticCalendarOptions").on("click", function() {
    var $portlet = $(this).closest(".Mrphs-container.Mrphs-sakai-summary-calendar");
    var $iframe = $(".Mrphs-toolBody.Mrphs-toolBody--sakai-summary-calendar iframe", $portlet);
    var $iframeBody = $($iframe[0].contentWindow.document.body);

    // trigger the options link
    $("#calendarForm .actionToolbar .firstToolBarItem a", $iframeBody).trigger("click");
  });
});


// Bind a dummy touch event on document to stop iOS from capturing
// a click to enable a hover state
$PBJQ(document).on("touchstart", function() { return true; });

