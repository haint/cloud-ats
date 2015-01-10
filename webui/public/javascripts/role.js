function checkCheckbox(){
  var countCheckbox = $("td :checkbox:checked").length;
  var name = $('input[name=name]').val();
  if(name != null && name != '' && countCheckbox ===0){
    $('.alert').show();
    return false;
  }
  return true;
}

$(document).ready(function() {
  $("#main").on("click", ".org-role-filter .form-search a.filter", function() {
    var ajaxURL = $(this).attr("ajax-url");
    
    var form = $(this).parent("form");
    form.find(":input").each(function(){
      if (this.value == '') this.disabled = true;
    })
    
    var values = form.serialize();
    form.find(":input:disabled").prop('disabled',false);
    
    var text = $(form).find(":input[value!='']").serialize();
    $.ajax({
      method: "GET",
      url: ajaxURL,
      data: values,
      dataType: "json",
      success: function(data) {
        $(".org-body .org-right table.org-role tr.role").hide();
        $(data.roles).each(function() {
          $(".role-" + this).show();
        });
      },
      error: function() {
        location.reload();
      }
    });
  });
  
  $("#main").on("keypress", ".org-role-filter .form-search input", function(e) {
    if (e.which == 13) {
      $(this).parent().find("a.filter").click();
    }
  });
  
  $("#main").on("keypress", ".org-right .form-horizontal input", function(e) {
    if (e.which == 13) {
      return false;
    }
  });
  
  $("#main").on("submit", ".org-role-filter .form-search", function() {
    return false;
  });
  $('#main').on('click','.form-actions .btn', function () {
    
  });
})