import React, {useContext} from "react";
import PageHeader from "../pageheader/PageHeader";
import {Section} from "./Section";
import source_data from "./source_data.png"
import source_code from "./source_code.png"
import {WebSocketStateContext} from "../ws/useWebSocketState";

export default function SourceTools() {
    const siteStatus = useContext(WebSocketStateContext);
    return <div id={"tools-component"} className={"container"}>
        <div>
            <PageHeader title={"Tools"}
                        breadcrumbs={
                            siteStatus.hasTarget ? [
                                    {to: "/tools", label: "Tools"},
                                    {label: "Source"},
                                ] :
                                undefined
                        }
            />

            <div className={"ms-8"}>
                <Section title={"Data"}
                         description={"Tools that apply data"}
                         to={"/tools/source/data"}
                         img={source_data}/>

                <Section title={"Code"}
                         description={"Tools that apply to code"}
                         to={"/tools/source/code"}
                         img={source_code}/>
            </div>
        </div>
    </div>
}

