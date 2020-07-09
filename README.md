# clojure-async-sandbox

Just me playing with clojure.core.async. I'm simulating a price computation engine. In order to simulate a cpu-bound price computation I'm doing some operations with matrices. Then I have two main programs, one that has a single event handler for computing prices for all products and another one that has one event handler per product. The latter uses closure to store the last computed price in an atom. I'm kinda implementing some sort of actor model here where each event handler represents an actor and we have one actor per product.

## Usage

To build the uberjar only:

    $ ./build.sh

To build and run with default args:

    $ ./build-and-run.sh

Run the following command to run both programs computing prices for 50 products:

    $ ./run.sh

## Options

    $ ./run.sh [mode] [number-of-products]

Where:

mode - sh or mh

number-of-products - any positive integer

## Examples

    $ ./run.sh sh 100

    $ ./run.sh mh 1000

### Bugs

Probably a lot.

### Disclaimer

This is a just a toy project so I can learn clojure.core.async and should not be seen as a production-grade solution.

## License

Copyright © 2020 JP Silva

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
