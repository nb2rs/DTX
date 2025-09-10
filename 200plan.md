# 2.0-0 plan
* Rollable hooks
  * A Base hook implementation that will handle some more modular-minded functionality
    * Will be things like "veto roll based on target?", "do this on veto" and "do this on roll completion"
  * Will have deeper implementations for Rollable implementors if necessary
* Builders
  * Builders. I love them.
  * A lot wider array of builders, all of which will either be a basal Rollable builder or basal RollableHooks builder.
* Consistency
  * I keep going over the code and fixing minor inconsistencies in style and where I do certain things and don't.
* Separate result selection and roll functions
  * Roll logic is more or less consistent throughout all implementations aside from roll selection
  * Therefore, the roll selection logic will be separated
* API Expansion
  * It will be more or less the same as before, 
