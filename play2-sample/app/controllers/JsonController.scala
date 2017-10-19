package controllers

import play.api.mvc._
import play.api.libs.json._
import scalikejdbc._
import models._

object JsonController {
  implicit val usersFormat = Json.format[Users]
  case class UserForm(id: Option[Long], name: String, companyId: Option[Int])
  implicit val userFormFormat = Json.format[UserForm]
  case class LoginForm(id: String, password: String)
  implicit val loginFormat = Json.format[LoginForm]
}

class JsonController extends Controller {
  import JsonController._

  // 一覧表示
  def list = Action { implicit request =>
    val u = Users.syntax("u")

    DB.readOnly { implicit session =>
      // ユーザのリストを取得
      val users = withSQL {
        select.from(Users as u).orderBy(u.id.asc)
      }.map(Users(u.resultName)).list.apply()
      Ok(Json.obj("users" -> users))
    }
  }

  // ユーザ登録
  def create = Action(parse.json) { implicit request =>
    request.body.validate[UserForm].map{ form =>
      // OKの場合はユーザを登録
      DB.localTx { implicit session =>
        Users.create(form.name, form.companyId)
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal{ e =>
      //　NGの場合はバリデーションエラーを返す
      BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e)))
    }
  }

  // ユーザ更新
  def update = Action(parse.json) { implicit request =>
    request.body.validate[UserForm].map { form =>
      // OKの場合はユーザ情報を更新
      DB.localTx { implicit session =>
        Users.find(form.id.get).foreach { user =>
          Users.save(user.copy(name = form.name, companyId = form.companyId))
        }
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e)))
    }
  }

  // ユーザ削除
  def remove(id: Long) = Action { implicit request =>
    DB.localTx { implicit session =>
      // ユーザを削除
      Users.find(id).foreach { user =>
        Users.destroy(user)
      }
      Ok(Json.obj("result" -> "success"))
    }
  }

  // テスト
  def test = Action(parse.json) { implicit request =>
    request.body.validate[LoginForm].map { form =>
      Ok(Json.obj("login ID" -> form.id, "login password" -> form.password))
    }.recoverTotal { e =>
      BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e)))
    }
  }
}
