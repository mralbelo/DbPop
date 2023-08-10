import React, {useContext} from "react";
import {Button} from "react-bootstrap";
import './CodeChanges.scss'
import PageHeader from "../pageheader/PageHeader";
import {uploadDbChangeToTarget, uploadFileChangeToTarget} from "../../api/codeApi";
import {ObjectIdentifier} from "../../models/ObjectIdentifier";
import {WebSocketStateContext} from "../ws/useWebSocketState";
import {NavLink} from "react-router-dom";

function Message({message}: { message: string }) {
    return <>
        <h5 className={"text-center"}>
            {message}
        </h5>
    </>
}

export default function CodeChanges() {
    const siteStatus = useContext(WebSocketStateContext);

    function onApplyFileChanges(path: string, objectIdentifier: ObjectIdentifier) {
        uploadFileChangeToTarget([objectIdentifier])
            .then(() => siteStatus.refreshCodeChanges());
    }

    function onApplyDbChanges(path: string, objectIdentifier: ObjectIdentifier) {
        uploadDbChangeToTarget([objectIdentifier])
            .then(() => siteStatus.refreshCodeChanges());
    }

    function onApplyAllFileChanges() {
        const applyChanges = siteStatus.codeChanges.map(value => value.objectIdentifier);
        uploadFileChangeToTarget(applyChanges)
            .then(() => siteStatus.refreshCodeChanges());
    }

    function onApplyAllDbChanges() {
        const applyChanges = siteStatus.codeChanges.map(value => value.objectIdentifier);
        uploadDbChangeToTarget(applyChanges)
            .then(() => siteStatus.refreshCodeChanges());
    }

    function ObjectIdentifierIcon({objectIdentifier}: { objectIdentifier: ObjectIdentifier }) {
        function icon(type: string) {
            if (type === 'SQL_STORED_PROCEDURE') return 'fa-code'
            if (type === 'USER_TABLE') return 'fa-table'
            return ''
        }

        function title(type: string) {
            if (type === 'SQL_STORED_PROCEDURE') return 'Stored Procedure'
            if (type === 'USER_TABLE') return 'Table'
            return ''
        }

        return <>
            <i className={`fa fa-fw ${icon(objectIdentifier.type)}`} title={title(objectIdentifier.type)}></i>&nbsp;
        </>
    }

    function ApplyAllBox() {
        return <>
            <div className="card">
                <div className="card-body">
                    <table>
                        <tbody>
                        {siteStatus.codeChanges.filter(it => it.databaseChanged || it.databaseDeleted).length > 0 &&
                            <tr>
                                <td>
                                    <Button variant={"primary"}
                                            size={"sm"}
                                            onClick={() => onApplyAllDbChanges()}
                                            className={"me-3"}
                                    >
                                        <i className={"fa fa-file"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-arrow-left"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-database"}/>
                                    </Button>
                                </td>
                                <td><strong>Apply All Database Changes</strong></td>
                            </tr>
                        }
                        {siteStatus.codeChanges.filter(it => it.fileChanged || it.fileDeleted).length > 0 &&
                            <tr>
                                <td>
                                    <Button variant={"outline-primary"}
                                            size={"sm"}
                                            onClick={() => onApplyAllFileChanges()}
                                            className={"me-2 mt-2"}
                                    >
                                        <i className={"fa fa-file"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-arrow-right"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-database"}/>
                                    </Button>
                                </td>
                                <td><strong>Apply All File Changes</strong></td>
                            </tr>
                        }
                        </tbody>
                    </table>
                </div>
            </div>
        </>
    }

    function ChangesBox() {
        return <>
            <div className="card mt-5">
                <div className="card-body">
                    <table className={"table table-hover"}>
                        <tbody>
                        {siteStatus.codeChanges.map(change => <tr key={change.objectIdentifier.type + '-' + change.dbname}>
                            <td width={"100%"}>
                                <NavLink to={"/codechanges/diff"} state={change.objectIdentifier}>
                                    <ObjectIdentifierIcon objectIdentifier={change.objectIdentifier}/>
                                    {change.dbname}
                                </NavLink>
                            </td>
                            <td width={"0"} style={{whiteSpace: "nowrap"}}>
                                {(change.fileChanged || change.fileDeleted) && <>
                                    <Button variant={"outline-primary"}
                                            size={"sm"}
                                            title={"Apply the source file change to the database"}
                                            onClick={() => onApplyFileChanges(change.path, change.objectIdentifier)}
                                    >
                                        <i className={"fa fa-file"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-arrow-right"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-database"}/>
                                    </Button>
                                </>
                                }
                                {(change.databaseChanged || change.databaseDeleted) && <>
                                    <Button variant={"primary"}
                                            size={"sm"}
                                            title={"Apply the database change to the source file"}
                                            onClick={() => onApplyDbChanges(change.path, change.objectIdentifier)}
                                    >
                                        <i className={"fa fa-file"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-arrow-left"} style={{paddingRight: "3px"}}/>
                                        <i className={"fa fa-database"}/>
                                    </Button>
                                </>}
                            </td>
                        </tr>)}
                        {siteStatus.codeChanges.length >= 100 && <tr>
                            <td colSpan={2}><Message message={"Too many changes to display"}/></td>
                        </tr>
                        }
                        </tbody>
                    </table>
                </div>
            </div>
        </>
    }

    function Content() {
        if (!siteStatus.hasCode) return <Message message={"No Code Downloaded"}/>
        if (siteStatus.codeChanges.length === 0) return <Message message={"No Changes Detected"}/>;
        return <>
            <div className={"row"}>
                <div className={"col-6"}>
                    <ApplyAllBox/>
                </div>
            </div>
            <ChangesBox/>
        </>
    }

    return <div className={"code-changes-component container"}>
        <PageHeader title={"Code Changes"} subtitle={"Track modified database code"}/>
        <Content/>
    </div>
}