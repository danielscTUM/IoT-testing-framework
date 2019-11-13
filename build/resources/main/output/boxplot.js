// append the svg object to the body of the page
function createBoxplot(target, data) {
  var margin = {top: 10, right: 20, bottom: 20, left: 40},
      svgWidth = 300 - margin.left - margin.right,
      svgHeight = 300 - margin.top - margin.bottom;
  var svg = d3.select(target)
  .append("svg")
  .attr("width", svgWidth + margin.left + margin.right)
  .attr("height", svgHeight + margin.top + margin.bottom)
  .append("g")
  .attr("transform",
      "translate(" + margin.left + "," + margin.top + ")");

  // create dummy data
  var q1 = data.q1;
  var median = data.median;
  var q3 = data.q3;
  var min = data.min;
  var max = data.max;

  // Show the Y scale
  var y = d3.scaleLinear()
  .domain([0,1.1 * max])
  .range([svgHeight, 0]);
  svg.call(d3.axisLeft(y));

  // a few features for the box
  var center = 150;
  var boxWidth = 50;

// Show the main vertical line
  svg
  .append("line")
  .attr("x1", center)
  .attr("x2", center)
  .attr("y1", y(min) )
  .attr("y2", y(max) )
  .attr("stroke", "black");

// Show the box
  svg
  .append("rect")
  .attr("x", center - boxWidth/2)
  .attr("y", y(q3) )
  .attr("height", (y(q1)-y(q3)) )
  .attr("width", boxWidth )
  .attr("stroke", "black")
  .style("fill", "#69b3a2");

  // show median, min and max horizontal lines
  svg
  .selectAll("toto")
  .data([min, median, max])
  .enter()
  .append("line")
  .attr("x1", center-boxWidth/2)
  .attr("x2", center+boxWidth/2)
  .attr("y1", function(d){ return(y(d))} )
  .attr("y2", function(d){ return(y(d))} )
  .attr("stroke", "black");
}
