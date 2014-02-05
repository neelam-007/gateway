From Layer 7 Release Notes v7.1 and v8.0
	Non-Layer 7 Custom Assertions May Fail to Load
		Customer-developed Custom Assertions may fail to load after upgrading to version 7.1 of the Gateway due to a change in the Custom Assertion SDK, causing an "Unknown assertion: CustomAssertion" error to occur.

		Specifically, it is not safe to include serialized versions of Layer 7 API classes within your own Custom Assertion classes. If you do so, your custom assertion may not work on future versions of the Gateway.

		If you have written a custom assertion where the actual assertion (implements CustomAssertion) class contains (directly or indirectly) a non-transient, non-static field with a value that is one of the Layer 7 API classes (such as VariableMetadata or DataType), then your custom assertion may not load on future versions of the Gateway.

		Note: Classes which implement CustomAssertion are serialized and stored inside the CustomAssertionHolder as Base-64 in the policy XML.

		If your assertion class has non-transient, non-static fields of type DataType or another class (such as VariableMetadata, which includes DataType), the assertion configuration will not load on a version 8.0 Gateway because the serialVersionUID of the DataType class has changed. Here is an example:
			public class FooAssertion implements CustomAssertion,SetsVariables {
				private VariableMetadata[] var = {};

				public VariableMetadata[] getVar() {
					return var;
				}

				public void setVar(VariableMetadata[] var) {
					this.var = var;
				}

				@Override
				public VariableMetadata[] getVariablesSet() {
					return var;
				}

				@Override
				public String getName() {
					return "Foo Assertion";
				}
			}

		Please note the following guidance:
			- This issue does not impact any custom assertions distributed by Layer 7 Technologies.
			- Customers who have not developed any in-house custom assertions should not be impacted by this issue and can upgrade normally.
			- Customers who have developed in-house custom assertions that do not contain code artifacts as noted above should not be impacted by this issue, but are advised to thoroughly test all policies containing their in-house assertions after upgrading to ensure there are no problems.
			- Customers who have developed in-house custom assertions that do contain the code artifacts noted above should contact Layer 7 Technical Support for information on resolution options.

***
The following is probably for internal Layer 7 use only.  As noted in the release note, the situation applies only to customers who have developed in-house custom assertions that contain Layer 7 API classes (such as VariableMetadata or DataType).

The following table describes upgrade compatibility for Custom Assertion saved using a previous SSG version.  Note this does not apply to SSG already previously upgraded (there's no way to verify which SSG version last saved a policy with a Custom Assertion).

Upgrade to->	SSG 6.x (SSG-4496 change to 	SSG 7.0		    SSG 7.1 (SSG-6467 	SSG 8.0 (SSG-6899 DataType 	    SSG post-8.0
		        VariableMetadata in SSG 6.0)			        change to DataType)	UID fix, SSG 7.0 compatible)	Icefish
------------	----------------------------	------------    -------------------	----------------------------	------------
SSG 5.x		    Incompatible			        Incompatible	Incompatible		Incompatible			        Incompatible
SSG 6.x		    N/A				                Compatible	    Incompatible		Compatible			            Compatible
SSG 7.0		    N/A				                N/A		        Incompatible		Compatible			            Compatible
SSG 7.1		    N/A				                N/A		        N/A			        Incompatible			        Incompatible
SSG 8.0		    N/A				                N/A		        N/A			        N/A				                Compatible