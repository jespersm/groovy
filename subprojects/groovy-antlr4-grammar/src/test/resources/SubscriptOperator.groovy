def someObject = [:]
someObject[0]
someObject['hey','ho']
someObject[0..10]
someObject[0..<11]

someObject[2,3] = '1'
someObject[[2,3]] = '2'
path.someObject[[2,3]] = '3'

someObject[some(0)]
someObject[]
someObject[0..10] = 'fish'
someObject[0..<11] = 'meh'

someObject[some(0), 42] = 127
