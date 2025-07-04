#![allow(missing_docs, unused_variables, trivial_casts)]


#[allow(unused_imports)]
use futures::{future, Stream, stream};
#[allow(unused_imports)]
use ops_v3::{Api, ApiNoContext, Claims, Client, ContextWrapperExt, models,
                      Op10GetResponse,
                      Op11GetResponse,
                      Op12GetResponse,
                      Op13GetResponse,
                      Op14GetResponse,
                      Op15GetResponse,
                      Op16GetResponse,
                      Op17GetResponse,
                      Op18GetResponse,
                      Op19GetResponse,
                      Op1GetResponse,
                      Op20GetResponse,
                      Op21GetResponse,
                      Op22GetResponse,
                      Op23GetResponse,
                      Op24GetResponse,
                      Op25GetResponse,
                      Op26GetResponse,
                      Op27GetResponse,
                      Op28GetResponse,
                      Op29GetResponse,
                      Op2GetResponse,
                      Op30GetResponse,
                      Op31GetResponse,
                      Op32GetResponse,
                      Op33GetResponse,
                      Op34GetResponse,
                      Op35GetResponse,
                      Op36GetResponse,
                      Op37GetResponse,
                      Op3GetResponse,
                      Op4GetResponse,
                      Op5GetResponse,
                      Op6GetResponse,
                      Op7GetResponse,
                      Op8GetResponse,
                      Op9GetResponse,
                     };
use clap::{Command, Arg};

// NOTE: Set environment variable RUST_LOG to the name of the executable (or "cargo run") to activate console logging for all loglevels.
//     See https://docs.rs/env_logger/latest/env_logger/  for more details

#[allow(unused_imports)]
use log::info;

// swagger::Has may be unused if there are no examples
#[allow(unused_imports)]
use swagger::{AuthData, ContextBuilder, EmptyContext, Has, Push, XSpanIdString};

type ClientContext = swagger::make_context_ty!(ContextBuilder, EmptyContext, Option<AuthData>, XSpanIdString);

mod client_auth;
use client_auth::build_token;


// rt may be unused if there are no examples
#[allow(unused_mut)]
fn main() {
    env_logger::init();

    let matches = Command::new("client")
        .arg(Arg::new("operation")
            .help("Sets the operation to run")
            .value_parser([
                "Op10Get",
                "Op11Get",
                "Op12Get",
                "Op13Get",
                "Op14Get",
                "Op15Get",
                "Op16Get",
                "Op17Get",
                "Op18Get",
                "Op19Get",
                "Op1Get",
                "Op20Get",
                "Op21Get",
                "Op22Get",
                "Op23Get",
                "Op24Get",
                "Op25Get",
                "Op26Get",
                "Op27Get",
                "Op28Get",
                "Op29Get",
                "Op2Get",
                "Op30Get",
                "Op31Get",
                "Op32Get",
                "Op33Get",
                "Op34Get",
                "Op35Get",
                "Op36Get",
                "Op37Get",
                "Op3Get",
                "Op4Get",
                "Op5Get",
                "Op6Get",
                "Op7Get",
                "Op8Get",
                "Op9Get",
            ])
            .required(true)
            .index(1))
        .arg(Arg::new("https")
            .long("https")
            .help("Whether to use HTTPS or not"))
        .arg(Arg::new("host")
            .long("host")
            .default_value("localhost")
            .help("Hostname to contact"))
        .arg(Arg::new("port")
            .long("port")
            .default_value("8080")
            .help("Port to contact"))
        .get_matches();

    // Create Bearer-token with a fixed key (secret) for test purposes.
    // In a real (production) system this Bearer token should be obtained via an external Identity/Authentication-server
    // Ensure that you set the correct algorithm and encodingkey that matches what is used on the server side.
    // See https://github.com/Keats/jsonwebtoken for more information
    let auth_token = build_token(
            Claims {
                sub: "tester@acme.com".to_owned(),
                company: "ACME".to_owned(),
                iss: "my_identity_provider".to_owned(),
                // added a very long expiry time
                aud: "org.acme.Resource_Server".to_string(),
                exp: 10000000000,
                // In this example code all available Scopes are added, so the current Bearer Token gets fully authorization.
                scopes:
                  "".to_owned()
            },
            b"secret").unwrap();

    let auth_data = if !auth_token.is_empty() {
        Some(AuthData::Bearer(auth_token))
    } else {
        // No Bearer-token available, so return None
        None
    };

    let is_https = matches.contains_id("https");
    let base_url = format!("{}://{}:{}",
        if is_https { "https" } else { "http" },
        matches.get_one::<String>("host").unwrap(),
        matches.get_one::<u16>("port").unwrap());

    let context: ClientContext =
        swagger::make_context!(ContextBuilder, EmptyContext, auth_data, XSpanIdString::default());

    let mut client : Box<dyn ApiNoContext<ClientContext>> = if is_https {
        // Using Simple HTTPS
        let client = Box::new(Client::try_new_https(&base_url)
            .expect("Failed to create HTTPS client"));
        Box::new(client.with_context(context))
    } else {
        // Using HTTP
        let client = Box::new(Client::try_new_http(
            &base_url)
            .expect("Failed to create HTTP client"));
        Box::new(client.with_context(context))
    };

    let mut rt = tokio::runtime::Runtime::new().unwrap();

    match matches.get_one::<String>("operation").map(String::as_str) {
        Some("Op10Get") => {
            let result = rt.block_on(client.op10_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op11Get") => {
            let result = rt.block_on(client.op11_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op12Get") => {
            let result = rt.block_on(client.op12_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op13Get") => {
            let result = rt.block_on(client.op13_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op14Get") => {
            let result = rt.block_on(client.op14_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op15Get") => {
            let result = rt.block_on(client.op15_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op16Get") => {
            let result = rt.block_on(client.op16_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op17Get") => {
            let result = rt.block_on(client.op17_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op18Get") => {
            let result = rt.block_on(client.op18_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op19Get") => {
            let result = rt.block_on(client.op19_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op1Get") => {
            let result = rt.block_on(client.op1_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op20Get") => {
            let result = rt.block_on(client.op20_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op21Get") => {
            let result = rt.block_on(client.op21_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op22Get") => {
            let result = rt.block_on(client.op22_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op23Get") => {
            let result = rt.block_on(client.op23_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op24Get") => {
            let result = rt.block_on(client.op24_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op25Get") => {
            let result = rt.block_on(client.op25_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op26Get") => {
            let result = rt.block_on(client.op26_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op27Get") => {
            let result = rt.block_on(client.op27_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op28Get") => {
            let result = rt.block_on(client.op28_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op29Get") => {
            let result = rt.block_on(client.op29_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op2Get") => {
            let result = rt.block_on(client.op2_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op30Get") => {
            let result = rt.block_on(client.op30_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op31Get") => {
            let result = rt.block_on(client.op31_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op32Get") => {
            let result = rt.block_on(client.op32_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op33Get") => {
            let result = rt.block_on(client.op33_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op34Get") => {
            let result = rt.block_on(client.op34_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op35Get") => {
            let result = rt.block_on(client.op35_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op36Get") => {
            let result = rt.block_on(client.op36_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op37Get") => {
            let result = rt.block_on(client.op37_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op3Get") => {
            let result = rt.block_on(client.op3_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op4Get") => {
            let result = rt.block_on(client.op4_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op5Get") => {
            let result = rt.block_on(client.op5_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op6Get") => {
            let result = rt.block_on(client.op6_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op7Get") => {
            let result = rt.block_on(client.op7_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op8Get") => {
            let result = rt.block_on(client.op8_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        Some("Op9Get") => {
            let result = rt.block_on(client.op9_get(
            ));
            info!("{:?} (X-Span-ID: {:?})", result, (client.context() as &dyn Has<XSpanIdString>).get().clone());
        },
        _ => {
            panic!("Invalid operation provided")
        }
    }
}
