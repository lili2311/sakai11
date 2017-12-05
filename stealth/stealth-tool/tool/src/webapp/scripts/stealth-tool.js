$(document).ready(function(){
	$("#select-term").select2();
	$("#select-tool").select2();
    $(".unstealth-multiple-netid").select2({
        placeholder:"NetID",
        minimumInputLength: 2,
    	ajax: {
    		url: function (params) {
    			console.log("Search string" + params.term);
    			return "http://localhost:8080/direct/stealth-admin/searchNetid/" + params.term;
    		},
	        delay: 500,
	        dataType: 'json'
       	}
    });
    $(".unstealth-multiple-siteid").select2({
        placeholder:"SiteID"
    });
});
function openTab(evt, TabName) {
	var i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
	   tabcontent[i].style.display = "none";
	}
	tablinks = document.getElementsByClassName("tablinks");
	for (i = 0; i < tablinks.length; i++) {
	   tablinks[i].className = tablinks[i].className.replace(" active", "");
	}
	document.getElementById(TabName).style.display = "block";
	evt.currentTarget.className += " active";
}