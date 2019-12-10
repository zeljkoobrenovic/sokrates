/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

ace.define("ace/snippets/verilog",["require","exports","module"], function(require, exports, module) {
"use strict";

exports.snippetText =undefined;
exports.scope = "verilog";

});                (function() {
                    ace.require(["ace/snippets/verilog"], function(m) {
                        if (typeof module == "object" && typeof exports == "object" && module) {
                            module.exports = m;
                        }
                    });
                })();
