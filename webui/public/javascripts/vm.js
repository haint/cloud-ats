$(document).ready(function() {
  /* Wizard
  ================================================== */
  $('.cloud.wizard').on('change', function (e, data) {
    if (data.direction === 'next') {
      $('.cloud.btn.prev').removeAttr('disabled');
    }
    if (data.direction === 'previous') {
      $('.cloud.btn.next').show();
      $('.cloud.btn.finish').hide();
    }
    if (data.step === 2 && data.direction === 'next') {
      $('.cloud.btn.next').hide();
      $('.cloud.btn.finish').show();
    }
  });
  
  $('.cloud.wizard').on('changed', function (e, data) {
    var item = $('.cloud.wizard').wizard('selectedItem');
    if (item.step === 1) {
      $('.cloud.btn.prev').attr("disabled", "disabled");
    }
    if (item.step !== 3) {
      $('.cloud.btn.next').show();
      $('.cloud.btn.finish').hide();
    }
  });
  
  $('.cloud.btn.finish').on('finished', function (e, data) {
  });
  
  $('.cloud.btn.prev').on('click', function () {
    var item = $('.cloud.wizard').wizard('selectedItem');
    if (item.step === 2) {
      $(this).attr("disabled", "disabled");
    }
    
    $('.cloud.wizard').wizard('previous');
  });
  
  $('.cloud.btn.next').on('click', function () {
  $('.cloud.wizard').wizard('next', 'foo');
  });

  /* Cloud VM list
   ===================================================== */
  $("body").on("click", ".cloud-vm a.plus", function() {
    var div = $(this).closest("tr").next("tr").find("div.vm-info");
    var icon = $(this).find("i.icon");
    
    if ($(div).css('display') == 'none') {
      $(div).slideDown(400);
      $(icon).removeClass("icon-plus");
      $(icon).addClass("icon-minus");
    } else {
      $(div).slideUp(400);
      $(icon).removeClass("icon-minus");
      $(icon).addClass("icon-plus");
    }
  });
  /* Click Terminal */
  $("body").on("click", ".cloud-vm a.tab-terminal", function() {
    var ajaxURL = $(this).attr("ajax-url");
    var terminal = $(this).closest("div.vm-info").find("div.vm-terminal");
    if ($(terminal).find("iframe").length == 0) {
      $.ajax({
        url: ajaxURL,
        dataType: "html",
        success: function(data) {
          $(terminal).html(data);
        },
        error: function(e, data) {
          console.log(e);
          console.log(data);
        }
      });
    }
  });
  
  /* Click VM Properties Update button*/
  $("body").on("click", ".cloud-vm button.update", function() {
    var data = $(this).attr("data");
    var form = $("#" + data);
    
    $(form).find("input").show();
    $(form).find("span.label").hide();
    
    $(this).hide();
    $(form).find("button.submit").show();
    $(form).find("button.cancel").show();
    
    $(form).find("input[type=password]").val("");
  });
  
  /* Click VM Properties Cancel button */
  $("body").on("click", ".cloud-vm button.cancel", function() {
    var data = $(this).attr("data");
    var form = $("#" + data);
    
    $(form).find("input").hide();
    $(form).find("span.label").show();
    
    $(this).hide();
    $(form).find("button.submit").hide();
    $(form).find("button.update").show();
    
    $(form).find("span.label").each(function() {
      $(this).next("input").val($(this).text());
    });
  })
  
  /* Click VM Properties Submit Button */
  $("body").on("click", ".cloud-vm button.submit", function(e) {
    var ajaxURL = $(this).attr("ajax-url");
    var form = $("#" + $(this).attr("data"));
    var vmId = $(form).find("input[name='vmId']").val();
    $.ajax({
      url: ajaxURL,
      method: "POST",
      dataType: "json",
      data: $(form).serialize(),
      success: function(data) {
        $("tr.vm-status-" + vmId).replaceWith(data.vmStatus);
        $("tr.vm-properties-" + vmId).replaceWith(data.vmProperties);
      }
    });
  });
  
  /* start vm */
  $("body").on("click", ".cloud-vm .vm-list .btn.stop,.btn.start,.btn.restore", function() {
    var href = $(this).attr("href");
    $.ajax({
      url: href,
      dataType: "html",
      success: function(data) {
        console.log("success");
      },
      error: function(error) {
        console.log(error);
      }
    });
    return false;
  });
  
  /* Validation
  =====================================================
  $("input,select,textarea").not("[type=submit]").jqBootstrapValidation({
   submitSuccess: function(form, event) {
     if ($(form).hasClass("vm-properties")) {
       event.stopPropagation();
       event.preventDefault();
       var action = $(form).attr("action");
       var parent = $(form).parent();
       $.ajax({
         url: action,
         method: "POST",
         dataType: "html",
         async: false,
         data: $(form).serialize(),
         success: function(data) {
           $(parent).html(data);
         }
       });
     }
   }
 });
  */
});