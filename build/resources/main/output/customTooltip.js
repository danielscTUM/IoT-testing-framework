function CustomTooltip () {}

CustomTooltip.prototype.init = function(params) {
  var eGui = this.eGui = document.createElement('div');
  eGui.classList.add('custom-tooltip');
  eGui.innerHTML = '<span>' + params.value + '</span>'
};

CustomTooltip.prototype.getGui = function() {
  return this.eGui;
};