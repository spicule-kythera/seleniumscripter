# Example Script

```yaml
# Setup the search - Populate the top 2 selects and enter the token value and wait for the auto complete to popup
0:
  operation: wait
  selector: id
  name: mcoveragetype
  timeout: 30
1:
  operation: select
  selector: id
  name: mcoveragetype
  selectBy: value
  value: 6
2:
  operation: select
  selector: id
  name: benefitYear
  selectBy: value
  value: 2021
3:
  operation: wait
  selector: id
  name: medicationname
  timeout: 30
4:
  operation: keys
  selector: id
  name: medicationname
  value: Zyvox
5:
  operation: wait
  selector: class
  name: ui-autocomplete
  timeout: 30
# Create an object called completelabels with the list output
6:
  operation: captureList
  selector: class
  name: ui-menu-item
  variable: autocompletelabels
# Loop - this takes my list object and a subscript to loop
7:
  operation: loop
  type: variable
  variable: autocompletelabels
  subscript: searchSubScript
# Subscripts - a collection of subscripts, could be more than one.
subscripts:
  searchSubScript:
  # Rather than searching the token, we take the value from the captureList
    1:
      operation: keys
      selector: id
      name: medicationname
      value: ${loopvalue}
    2:
      operation: try
      operation: wait
      selector: class
      name: ui-autocomplete
      timeout: 30
    3:
      # Because of the popup autocomplete list, this gets a list and then selects the element numbered in the 3rd block, because we've searched for the specific drug, we know what we're looking for is always 0 in the list.
      operation: clickListItem
      selector: class
      name: ui-menu-item
      item: 0
    4:
      operation: click
      selector: class
      name: btn-find-provider
    5:
      # Because of the loop, here is the snapshot stuff we discussed
      operation: snapshot
```